package com.ryanharter.auto.value.parcel.util;

import java.util.Locale;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public final class TestProcessingEnvironment implements ProcessingEnvironment {

  private final Messager messager;
  private final Elements elements;
  private final Types types;

  public TestProcessingEnvironment(Messager messager, Elements elements, Types types) {
    this.messager = messager;
    this.elements = elements;
    this.types = types;
  }

  @Override
  public Map<String, String> getOptions() {
    return null;
  }

  @Override
  public Messager getMessager() {
    return messager;
  }

  @Override
  public Filer getFiler() {
    return null;
  }

  @Override
  public Elements getElementUtils() {
    return elements;
  }

  @Override
  public Types getTypeUtils() {
    return types;
  }

  @Override
  public SourceVersion getSourceVersion() {
    return null;
  }

  @Override
  public Locale getLocale() {
    return null;
  }
}
