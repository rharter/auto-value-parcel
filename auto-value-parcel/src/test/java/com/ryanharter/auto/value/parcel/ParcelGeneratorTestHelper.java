package com.ryanharter.auto.value.parcel;

import com.google.testing.compile.CompilationRule;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class ParcelGeneratorTestHelper {
  Types types;
  Elements elements;

  public ParcelGeneratorTestHelper(CompilationRule rule) {
    types = rule.getTypes();
    elements = rule.getElements();
  }

  public Types getTypes() {
    return types;
  }

  public Elements getElements() {
    return elements;
  }

  TypeElement getTypeElement(Class<?> cls) {
    return elements.getTypeElement(cls.getCanonicalName());
  }

  ExecutableElement getExecutableElement(Class<?> cls, String name) {
    TypeElement element = getTypeElement(cls);
    for (Element el : element.getEnclosedElements()) {
      if (el instanceof ExecutableElement) {
        ExecutableElement ee = (ExecutableElement) el;
        Name simpleName = ee.getSimpleName();
        if (simpleName != null && simpleName.contentEquals(name)) {
          return ee;
        }
      }
    }
    return null;
  }
}