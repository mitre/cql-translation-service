package org.mitre.bonnie.cqlTranslationServer;

import java.io.File;
import java.io.IOException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslator.Options;
import org.cqframework.cql.cql2elm.LibraryManager;

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
  private final LibraryManager libraryManager;
  
  public TranslationResource() {
    this.libraryManager = new LibraryManager();
  }
  
  @POST
  @Consumes(CQL_TEXT_TYPE)
  @Produces(ELM_XML_TYPE)
  public Response cqlToElmXml(File cql) {
    CqlTranslator translator = getTranslator(cql);
    ResponseBuilder resp = getResponse(translator);
    resp = resp.entity(translator.toXml()).type(ELM_XML_TYPE);
    return resp.build();
  }

  @POST
  @Consumes(CQL_TEXT_TYPE)
  @Produces(ELM_JSON_TYPE)
  public Response cqlToElmJson(File cql) {
    CqlTranslator translator = getTranslator(cql);
    ResponseBuilder resp = getResponse(translator);
    resp = resp.entity(translator.toJson()).type(ELM_JSON_TYPE);
    return resp.build();
  }

  private CqlTranslator getTranslator(File cql) {
    try {
      //LibrarySourceLoader.registerProvider(
      //        new DefaultLibrarySourceProvider(cql.toPath().getParent()));
      Options options[] = {Options.EnableAnnotations};
      return CqlTranslator.fromFile(cql, libraryManager, options);
      //LibrarySourceLoader.clearProviders();
    } catch (IOException e) {
      throw new TranslationFailureException("Unable to read request");
    }
  }

  private ResponseBuilder getResponse(CqlTranslator translator) {
    ResponseBuilder resp = translator.getErrors().size() > 0
            ? Response.status(Status.BAD_REQUEST) : Response.ok();
    return resp;
  }

}
