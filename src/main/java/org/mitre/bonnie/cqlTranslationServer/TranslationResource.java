package org.mitre.bonnie.cqlTranslationServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.cql2elm.DefaultLibrarySourceProvider;
import org.cqframework.cql.cql2elm.LibrarySourceLoader;
import org.cqframework.cql.elm.tracking.TrackBack;

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
  public String translate(File cql) {
    try {
      //LibrarySourceLoader.registerProvider(
      //        new DefaultLibrarySourceProvider(cql.toPath().getParent()));
      // TODO: Add options ?
      CqlTranslator translator = CqlTranslator.fromFile(cql);
      //LibrarySourceLoader.clearProviders();
      
      if (translator.getErrors().size() > 0) {
        throw new TranslationFailureException(translator.getErrors());
      }
      return translator.toXml();
    } catch (IOException ex) {
      throw new TranslationFailureException("Unable to read request");
    }
  }
}
