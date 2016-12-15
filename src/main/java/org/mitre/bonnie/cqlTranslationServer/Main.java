package org.mitre.bonnie.cqlTranslationServer;

import java.io.IOException;
import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ServerProperties;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.CommandLine;

/**
 * Main class.
 *
 */
public class Main {
  // Base URI the Grizzly HTTP server will listen on

  // Must be 0.0.0.0 and not "localhost" to allow binding to other available network interfaces.
  public static final String BASE_URI = "http://0.0.0.0:8080/cql/";

  /**
   * Starts Grizzly HTTP server exposing JAX-RS resources defined in this
   * application.
   *
   * @return Grizzly HTTP server.
   */
  public static HttpServer startServer() {
    // create a resource config that scans for JAX-RS resources and providers
    // in org.mitre.bonnie.cqlTranslationServer package
    final ResourceConfig rc = new ResourceConfig().packages("org.mitre.bonnie.cqlTranslationServer");
    rc.property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, "true");
    rc.register(MultiPartFeature.class);

    // create and start a new instance of grizzly http server
    // exposing the Jersey application at BASE_URI
    return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
  }

  /**
   * Main method.
   *
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    final HttpServer server = startServer();
    
    Options options = new Options();
    options.addOption("d", false, "Daemon mode, doesn't wait input from command line");
    CommandLineParser parser = new DefaultParser();
    
    try {
      CommandLine cmd = parser.parse( options, args);
    
      if (!cmd.hasOption("d")) {
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
        System.in.read();
        server.shutdownNow();
      }
    }
    catch (ParseException e) {
      System.err.println( "Unable to parse command line arguments: " + e.getMessage());
      server.shutdownNow();
    }
  }
}
