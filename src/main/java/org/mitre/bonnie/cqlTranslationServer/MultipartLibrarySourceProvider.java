package org.mitre.bonnie.cqlTranslationServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.gen.cqlBaseVisitor;
import org.cqframework.cql.gen.cqlLexer;
import org.cqframework.cql.gen.cqlParser;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.hl7.elm.r1.VersionedIdentifier;

/**
 * LibrarySourceProvider for FormDataMultiPart packaged set of CQL files
 *
 * @author mhadley
 */
public class MultipartLibrarySourceProvider implements LibrarySourceProvider {

  private final Map<VersionedIdentifier, BodyPart> pkgContents;

  public MultipartLibrarySourceProvider(FormDataMultiPart pkg) throws IOException {
    pkgContents = new HashMap<>();
    for (BodyPart part: pkg.getBodyParts()) {
      cqlLexer lexer = new cqlLexer(new ANTLRInputStream(part.getEntityAs(InputStream.class)));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      cqlParser parser = new cqlParser(tokens);
      parser.setBuildParseTree(true);
      ParseTree tree = parser.library();
      CqlLibraryIdentificationVisitor visitor = new CqlLibraryIdentificationVisitor();
      visitor.visit(tree);
      if (visitor.getLibraryId() != null)
        pkgContents.put(visitor.getLibraryId(), part);
    }
  }

  @Override
  public InputStream getLibrarySource(VersionedIdentifier vi) {
    BodyPart part = pkgContents.get(vi);
    if (part==null)
      throw new IllegalArgumentException(String.format("Could not load source for library %s.", vi.getId()));
    return part.getEntityAs(InputStream.class);
  }

  /**
   * Minimal ANTLR visitor for CQL that simply extracts the library ID for a
   * CQL file.
   */
  private static class CqlLibraryIdentificationVisitor extends cqlBaseVisitor {

    private final org.hl7.elm.r1.ObjectFactory of = new org.hl7.elm.r1.ObjectFactory();
    private VersionedIdentifier vid = null;

    public VersionedIdentifier getLibraryId() {
      return vid;
    } 

    @Override
    public VersionedIdentifier visitLibraryDefinition(@NotNull cqlParser.LibraryDefinitionContext ctx) {
      vid = of.createVersionedIdentifier()
              .withId(parseString(ctx.identifier()))
              .withVersion(parseString(ctx.versionSpecifier()));
      return vid;
    }
    
    @Override
    public Object visitTerminal(@NotNull TerminalNode node) {
      String text = node.getText();
      int tokenType = node.getSymbol().getType();
      if (cqlLexer.STRING == tokenType || cqlLexer.QUOTEDIDENTIFIER == tokenType) {
        // chop off leading and trailing ' or "
        text = text.substring(1, text.length() - 1);
      }

      return text;
    }

    private String parseString(ParseTree pt) {
      return pt == null ? null : (String) visit(pt);
    }
  }
}
