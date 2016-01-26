package org.mitre.bonnie.cqlTranslationServer;

import java.io.InputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
  public String translate(InputStream cql) {
    return "working on it";
  }
}
