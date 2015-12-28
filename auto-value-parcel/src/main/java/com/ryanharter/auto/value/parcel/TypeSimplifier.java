package com.ryanharter.auto.value.parcel;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

final class TypeSimplifier {

  private TypeSimplifier() {
  }

  static boolean isClassOfType(Types typeUtils, TypeMirror type, TypeMirror cls) {
    return type != null && typeUtils.isAssignable(cls, type);
  }
}
