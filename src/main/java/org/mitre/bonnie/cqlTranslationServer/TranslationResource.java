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

/**
 * Root resource (exposed at "translator" path)
 */
@Path("translator")
public class TranslationResource {

  /**
   * Method handling HTTP GET requests. The returned object will be sent to the
   * client as "text/plain" media type.
   *
   * @return String that will be returned as a text/plain response.
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String getIt() {
    return "Got it!";
  }

  @POST
  @Consumes("application/cql")
  @Produces("application/elm+xml")
  public Response cqlToElmXml(File cql) {
    try {
      //LibrarySourceLoader.registerProvider(
      //        new DefaultLibrarySourceProvider(cql.toPath().getParent()));
      Options options[] = {Options.EnableAnnotations};
      CqlTranslator translator = CqlTranslator.fromFile(cql, options);
      //LibrarySourceLoader.clearProviders();
      
      ResponseBuilder resp = translator.getErrors().size() > 0 ? 
              Response.status(Status.BAD_REQUEST) : Response.ok();
      resp = resp.entity(translator.toXml()).type("application/elm+xml");
      return resp.build();
    } catch (IOException ex) {
      throw new TranslationFailureException("Unable to read request");
    }
  }

  @POST
  @Consumes("application/cql")
  @Produces("application/elm+json")
  public Response cqlToElmJson(File cql) {
    try {
      //LibrarySourceLoader.registerProvider(
      //        new DefaultLibrarySourceProvider(cql.toPath().getParent()));
      Options options[] = {Options.EnableAnnotations};
      CqlTranslator translator = CqlTranslator.fromFile(cql, options);
      //LibrarySourceLoader.clearProviders();
      
      ResponseBuilder resp = translator.getErrors().size() > 0 ? 
              Response.status(Status.BAD_REQUEST) : Response.ok();
      resp = resp.entity(translator.toJson()).type("application/elm+xml");
      return resp.build();
    } catch (IOException ex) {
      throw new TranslationFailureException("Unable to read request");
    }
  }

}
