package org.mitre.bonnie.cqlTranslationServer;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class TranslationResourceTest {

  private HttpServer server;
  private WebTarget target;

  @Before
  public void setUp() throws Exception {
    // start the server
    server = Main.startServer();
    // create the client
    Client c = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();

    // uncomment the following line if you want to enable
    // support for JSON in the client (you also have to uncomment
    // dependency on jersey-media-json module in pom.xml and Main.startServer())
    // --
    // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());
    target = c.target(Main.BASE_URI);
  }

  @After
  public void tearDown() throws Exception {
    server.shutdownNow();
  }

  private static Document parseXml(File f) throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    domFactory.setNamespaceAware(true);
    DocumentBuilder builder = domFactory.newDocumentBuilder();
    return builder.parse(f);
  }

  private static String applyXPath(Document doc, String xPathStr) throws XPathExpressionException {
    XPath xpath = XPathFactory.newInstance().newXPath();
    xpath.setNamespaceContext(new ElmNamespaceContext());
    XPathExpression expr = xpath.compile(xPathStr);
    Object result = expr.evaluate(doc, XPathConstants.STRING);
    return (String) result;
  }

  @Test
  public void testInvalidCqlAsXml() {
    File file = new File(TranslationResourceTest.class.getResource("invalid.cql").getFile());
    Response resp = target.path("translator").request(TranslationResource.ELM_XML_TYPE).post(Entity.entity(file, TranslationResource.CQL_TEXT_TYPE));
    assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    assertEquals(TranslationResource.ELM_XML_TYPE, resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    try {
      Document doc = parseXml(resp.readEntity(File.class));
      String errorCount = applyXPath(doc, "count(/elm:library/elm:annotation[@errorType='syntax'])");
      assertEquals(1, Integer.parseInt(errorCount));
      String errorLine = applyXPath(doc, "/elm:library/elm:annotation[@errorType='syntax'][1]/@startLine");
      assertEquals(1, Integer.parseInt(errorLine));
      String errorChar = applyXPath(doc, "/elm:library/elm:annotation[@errorType='syntax'][1]/@startChar");
      assertEquals(0, Integer.parseInt(errorChar));
    } catch (Exception ex) {
      fail("Error parsing returned XML");
    }
  }

  @Test
  public void testInvalidCqlAsJson() {
    File file = new File(TranslationResourceTest.class.getResource("invalid.cql").getFile());
    Response resp = target.path("translator").request(TranslationResource.ELM_JSON_TYPE).post(Entity.entity(file, TranslationResource.CQL_TEXT_TYPE));
    assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    assertEquals(TranslationResource.ELM_JSON_TYPE, resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    JsonReader reader = Json.createReader(new StringReader(resp.readEntity(String.class)));
    JsonObject obj = reader.readObject();
    JsonObject library = obj.getJsonObject("library");
    JsonArray annotations = library.getJsonArray("annotation");
    assertEquals(1, annotations.size());
    JsonObject errorAnnotation = annotations.getJsonObject(0);
    assertEquals("CqlToElmError", errorAnnotation.getString("type"));
    assertEquals("syntax", errorAnnotation.getString("errorType"));
    assertEquals(1, errorAnnotation.getInt("startLine"));
    assertEquals(0, errorAnnotation.getInt("startChar"));
  }

  @Test
  public void testMissingLibraryAsJson() {
    File file = new File(TranslationResourceTest.class.getResource("missingLibrary.cql").getFile());
    Response resp = target.path("translator").request(TranslationResource.ELM_JSON_TYPE).post(Entity.entity(file, TranslationResource.CQL_TEXT_TYPE));
    assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    assertEquals(TranslationResource.ELM_JSON_TYPE, resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    JsonReader reader = Json.createReader(new StringReader(resp.readEntity(String.class)));
    JsonObject obj = reader.readObject();
    JsonObject library = obj.getJsonObject("library");
    JsonArray annotations = library.getJsonArray("annotation");
    assertEquals(1, annotations.size());
    JsonObject errorAnnotation = annotations.getJsonObject(0);
    assertEquals("CqlToElmError", errorAnnotation.getString("type"));
    assertEquals("include", errorAnnotation.getString("errorType"));
    assertEquals(5, errorAnnotation.getInt("startLine"));
    assertEquals(1, errorAnnotation.getInt("startChar"));
    assertEquals("CMS146", errorAnnotation.getString("library"));
    assertEquals("CMSAll", errorAnnotation.getString("targetIncludeLibraryId"));
    assertEquals("1", errorAnnotation.getString("targetIncludeLibraryVersionId"));
  }

  @Test
  public void testValidLibraryAsJson() {
    File file = new File(TranslationResourceTest.class.getResource("valid.cql").getFile());
    Response resp = target.path("translator").request(TranslationResource.ELM_JSON_TYPE).post(Entity.entity(file, TranslationResource.CQL_TEXT_TYPE));
    assertEquals(Status.OK.getStatusCode(), resp.getStatus());
    assertEquals(TranslationResource.ELM_JSON_TYPE, resp.getMediaType().toString());
    assertTrue(resp.hasEntity());
    JsonReader reader = Json.createReader(new StringReader(resp.readEntity(String.class)));
    JsonObject obj = reader.readObject();
    JsonObject library = obj.getJsonObject("library");
    JsonArray annotations = library.getJsonArray("annotation");
    assertNull(annotations);
    JsonObject identifier = library.getJsonObject("identifier");
    assertEquals("CMS146", identifier.getString("id"));
    assertEquals("2", identifier.getString("version"));
  }
  
  @Test
  public void testSingleLibraryAsMultipart() {
    File file = new File(TranslationResourceTest.class.getResource("valid.cql").getFile());
    FormDataMultiPart pkg = new FormDataMultiPart();
    pkg.field("foo", file, new MediaType("application", "cql"));
    Response resp = target.path("translator").request(MediaType.MULTIPART_FORM_DATA).post(Entity.entity(pkg, MediaType.MULTIPART_FORM_DATA));
    assertEquals(Status.OK.getStatusCode(), resp.getStatus());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getType(), resp.getMediaType().getType());
    assertEquals(MediaType.MULTIPART_FORM_DATA_TYPE.getSubtype(), resp.getMediaType().getSubtype());
    assertTrue(resp.hasEntity());
    FormDataMultiPart translatedPkg = resp.readEntity(FormDataMultiPart.class);
    assertEquals(1, translatedPkg.getBodyParts().size());
    assertEquals(1, translatedPkg.getFields("foo").size());
    JsonReader reader = Json.createReader(new StringReader(translatedPkg.getBodyParts().get(0).getEntityAs(String.class)));
    JsonObject obj = reader.readObject();
    JsonObject library = obj.getJsonObject("library");
    JsonArray annotations = library.getJsonArray("annotation");
    assertNull(annotations);
    JsonObject identifier = library.getJsonObject("identifier");
    assertEquals("CMS146", identifier.getString("id"));
    assertEquals("2", identifier.getString("version"));    
  }

  private static class ElmNamespaceContext implements NamespaceContext {

    private final Map<String, String> ns = new HashMap<>();

    public ElmNamespaceContext() {
      ns.put("elm", "urn:hl7-org:elm:r1");
      ns.put("t", "urn:hl7-org:elm-types:r1");
      ns.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
      ns.put("xsd", "http://www.w3.org/2001/XMLSchema");
      ns.put("quick", "http://hl7.org/fhir");
      ns.put("a", "urn:hl7-org:cql-annotations:r1");
    }

    @Override
    public String getNamespaceURI(String prefix) {
      return ns.get(prefix);
    }

    @Override
    public String getPrefix(String uri) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator getPrefixes(String uri) {
      throw new UnsupportedOperationException();
    }
  }
}
