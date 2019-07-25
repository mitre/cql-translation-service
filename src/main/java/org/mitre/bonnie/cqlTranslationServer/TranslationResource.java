package org.mitre.bonnie.cqlTranslationServer;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslator.Options;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.fhir.ucum.UcumEssenceService;
import org.fhir.ucum.UcumException;
import org.fhir.ucum.UcumService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

/**
 * Root resource (exposed at "translator" path). Uses default per-request
 * life cycle so a new CQL LibraryManager is instantiated and used for each
 * request that can include a batch of related CQL files.
 */
@Path("translator")
public class TranslationResource {

  public static final String CQL_TEXT_TYPE = "application/cql";
  public static final String ELM_XML_TYPE = "application/elm+xml";
  public static final String ELM_JSON_TYPE = "application/elm+json";
  public static final MultivaluedMap<String, Options> PARAMS_TO_OPTIONS_MAP = new MultivaluedHashMap<String, Options>() {{
    putSingle("date-range-optimization", Options.EnableDateRangeOptimization);
    putSingle("annotations", Options.EnableAnnotations);
    putSingle("locators", Options.EnableLocators);
    putSingle("result-types", Options.EnableResultTypes);
    putSingle("detailed-errors", Options.EnableDetailedErrors);
    putSingle("disable-list-traversal", Options.DisableListTraversal);
    putSingle("disable-list-demotion", Options.DisableListDemotion);
    putSingle("disable-list-promotion", Options.DisableListPromotion);
    putSingle("enable-interval-demotion", Options.EnableIntervalDemotion);
    putSingle("enable-interval-promotion", Options.EnableIntervalPromotion);
    putSingle("disable-method-invocation", Options.DisableMethodInvocation);
    putSingle("require-from-keyword", Options.RequireFromKeyword);
    put("strict", Arrays.asList(
            Options.DisableListTraversal,
            Options.DisableListDemotion,
            Options.DisableListPromotion,
            Options.DisableMethodInvocation)
    );
    put("debug", Arrays.asList(
            Options.EnableAnnotations,
            Options.EnableLocators,
            Options.EnableResultTypes)
    );
  }};
  private final ModelManager modelManager;
  private final LibraryManager libraryManager;

  public TranslationResource() {
    this.modelManager = new ModelManager();
    this.libraryManager = new LibraryManager(modelManager);
  }

  @POST
  @Consumes(CQL_TEXT_TYPE)
  @Produces(ELM_XML_TYPE)
  public Response cqlToElmXml(File cql, @Context UriInfo info) {
    try {
      libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
      CqlTranslator translator = getTranslator(cql, info.getQueryParameters());
      ResponseBuilder resp = getResponse(translator);
      resp = resp.entity(translator.toXml()).type(ELM_XML_TYPE);
      return resp.build();
    } finally {
      libraryManager.getLibrarySourceLoader().clearProviders();
    }
  }

  @POST
  @Consumes(CQL_TEXT_TYPE)
  @Produces(ELM_JSON_TYPE)
  public Response cqlToElmJson(File cql, @Context UriInfo info) {
    try {
      libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
      CqlTranslator translator = getTranslator(cql, info.getQueryParameters());
      ResponseBuilder resp = getResponse(translator);
      resp = resp.entity(translator.toJson()).type(ELM_JSON_TYPE);
      return resp.build();
    } finally {
      libraryManager.getLibrarySourceLoader().clearProviders();
    }
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.MULTIPART_FORM_DATA)
  public Response cqlPackageToElmPackage(
          FormDataMultiPart pkg,
          @HeaderParam("X-TargetFormat") @DefaultValue(ELM_JSON_TYPE) MediaType targetFormat,
          @Context UriInfo info
  ) {
    try {
      // note: if FhirLibrarySourceProvider isn't registered first it doesn't seem to work
      libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
      MultipartLibrarySourceProvider lsp = new MultipartLibrarySourceProvider(pkg);
      libraryManager.getLibrarySourceLoader().registerProvider(lsp);
      FormDataMultiPart translatedPkg = new FormDataMultiPart();
      for (String fieldId: pkg.getFields().keySet()) {
        for (FormDataBodyPart part: pkg.getFields(fieldId)) {
          CqlTranslator translator = getTranslator(part.getEntityAs(File.class), info.getQueryParameters());
          if (targetFormat.equals(MediaType.valueOf(ELM_XML_TYPE))) {
            translatedPkg.field(fieldId, translator.toXml(), targetFormat);
          } else {
            translatedPkg.field(fieldId, translator.toJson(), targetFormat);
          }
        }
      }
      ResponseBuilder resp = Response.ok().type(MediaType.MULTIPART_FORM_DATA).entity(translatedPkg);
      return resp.build();
    } catch (IOException ex) {
      throw new TranslationFailureException("Unable to read request");
    } finally {
      libraryManager.getLibrarySourceLoader().clearProviders();
    }
  }

  private CqlTranslator getTranslator(File cql, MultivaluedMap<String, String> params) {
    try {
      //LibrarySourceLoader.registerProvider(
      //        new DefaultLibrarySourceProvider(cql.toPath().getParent()));
      UcumService ucumService = null;
      LibraryBuilder.SignatureLevel signatureLevel = LibraryBuilder.SignatureLevel.None;
      List<Options> optionsList = new ArrayList<>();
      for (String key: params.keySet()) {
        if (PARAMS_TO_OPTIONS_MAP.containsKey(key) && Boolean.parseBoolean(params.getFirst(key))) {
          optionsList.addAll(PARAMS_TO_OPTIONS_MAP.get(key));
        } else if (key.equals("validate-units") && Boolean.parseBoolean(params.getFirst(key))) {
          try {
            ucumService = new UcumEssenceService(UcumEssenceService.class.getResourceAsStream("/ucum-essence.xml"));
          } catch (UcumException e) {
            throw new TranslationFailureException("Cannot load UCUM service to validate units");
          }
        } else if (key.equals("signatures")) {
          signatureLevel = LibraryBuilder.SignatureLevel.valueOf(params.getFirst("signatures"));
        }
      }
      Options[] options = optionsList.toArray(new Options[optionsList.size()]);
      return CqlTranslator.fromFile(cql, modelManager, libraryManager, ucumService, CqlTranslatorException.ErrorSeverity.Info,
              signatureLevel, options);
      //LibrarySourceLoader.clearProviders();
    } catch (IOException e) {
      throw new TranslationFailureException("Unable to read request");
    }
  }

  private ResponseBuilder getResponse(CqlTranslator translator) {
    return translator.getErrors().size() > 0
            ? Response.status(Status.BAD_REQUEST) : Response.ok();
  }

}
