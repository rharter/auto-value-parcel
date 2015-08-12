package com.ryanharter.autoparcel.util;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Created by rharter on 8/12/15.
 */
public final class TestMessager implements Messager {

  @Override
  public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
    printMessage(kind, msg, null);
  }

  @Override
  public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
    printMessage(kind, msg, e, null);
  }

  @Override
  public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
    printMessage(kind, msg, e, a, null);
  }

  @Override
  public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
    PrintStream out;
    if (kind == Diagnostic.Kind.ERROR) {
      out = System.err;
    } else {
      out = System.out;
    }
    out.println(msg);
  }
}
