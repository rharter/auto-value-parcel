package com.ryanharter.autoparcel;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

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
