package com.ryanharter.auto.value.parcel;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public final class TypeSimplifier {

  private TypeSimplifier() {
  }

  static boolean isClassOfType(Types typeUtils, TypeMirror type, TypeMirror cls) {
    return type != null && typeUtils.isAssignable(cls, type);
  }

  static TypeMirror typeFromClass(Types types, Elements elements, Class<?> cls) {
    if (cls == void.class) {
      return types.getNoType(TypeKind.VOID);
    } else if (cls.isPrimitive()) {
      String primitiveName = cls.getName().toUpperCase();
      TypeKind primitiveKind = TypeKind.valueOf(primitiveName);
      return types.getPrimitiveType(primitiveKind);
    } else if (cls.isArray()) {
      TypeMirror componentType = typeFromClass(types, elements, cls.getComponentType());
      return types.getArrayType(componentType);
    } else {
      TypeElement element = elements.getTypeElement(cls.getCanonicalName());
      if (element == null) {
        throw new IllegalArgumentException("Unknown type: " + cls.getCanonicalName());
      }
      return element.asType();
    }
  }

}
