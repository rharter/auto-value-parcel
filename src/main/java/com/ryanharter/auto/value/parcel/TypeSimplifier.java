package com.ryanharter.auto.value.parcel;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Created by rharter on 6/2/15.
 */
public class TypeSimplifier {

  /**
   * Returns the name of the given type, including any enclosing types but not the package.
   */
  static String classNameOf(TypeElement type) {
    String name = type.getQualifiedName().toString();
    String pkgName = packageNameOf(type);
    return pkgName.isEmpty() ? name : name.substring(pkgName.length() + 1);
  }

  /**
   * Returns the name of the package that the given type is in. If the type is in the default
   * (unnamed) package then the name is the empty string.
   */
  static String packageNameOf(TypeElement type) {
    while (true) {
      Element enclosing = type.getEnclosingElement();
      if (enclosing instanceof PackageElement) {
        return ((PackageElement) enclosing).getQualifiedName().toString();
      }
      type = (TypeElement) enclosing;
    }
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

  static String simpleNameOf(String s) {
    if (s.contains(".")) {
      return s.substring(s.lastIndexOf('.') + 1);
    } else {
      return s;
    }
  }

  static String packageNameOf(String s) {
    if (s.contains(".")) {
      return s.substring(0, s.lastIndexOf('.'));
    } else {
      return "";
    }
  }
}
