/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mitre.bonnie.cqlTranslationServer;

import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.cqframework.cql.cql2elm.CqlTranslatorException;
import org.cqframework.cql.elm.tracking.TrackBack;

/**
 *
 * @author mhadley
 */
public class TranslationFailureException extends WebApplicationException {

  private static final long serialVersionUID = 3188788471978609249L;

  public TranslationFailureException(String msg) {
    super(Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN_TYPE)
            .entity(msg)
            .build());
  }

  public TranslationFailureException(List<CqlTranslatorException> translationErrs) {
    super(Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN_TYPE)
            .entity(formatMsg(translationErrs))
            .build());
  }

  private static String formatMsg(List<CqlTranslatorException> translationErrs) {
    StringBuilder msg = new StringBuilder();
    msg.append("Translation failed due to errors:");
    for (CqlTranslatorException error : translationErrs) {
      TrackBack tb = error.getLocator();
      String lines = tb == null ? "[n/a]" : String.format("[%d:%d, %d:%d]",
              tb.getStartLine(), tb.getStartChar(), tb.getEndLine(),
              tb.getEndChar());
      msg.append(String.format("%s %s%n", lines, error.getMessage()));
    }
    return msg.toString();
  }
}
