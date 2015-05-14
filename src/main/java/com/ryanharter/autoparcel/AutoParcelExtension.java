package com.ryanharter.autoparcel;

import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValueExtension;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.*;

public class AutoParcelExtension implements AutoValueExtension {

  Context context;

  Messager getMessager(Context context) {
    return context.processingEnvironment().getMessager();
  }

  @Override
  public boolean applicable(Context context) {
    TypeElement parcelable = context.processingEnvironment().getElementUtils().getTypeElement("android.os.Parcelable");
    return parcelable != null &&
        context.processingEnvironment().getTypeUtils().isAssignable(context.autoValueClass().asType(), parcelable.asType());
  }

  @Override
  public AutoValueExtension.GeneratedClass generateClass(final Context context, final String className, final String classToExtend, String classToImplement) {
    this.context = context;
    return new AutoValueExtension.GeneratedClass() {

      @Override
      public String className() {
        return className;
      }

      @Override
      public String source() {
        return null;
      }

      @Override
      public Collection<ExecutableElement> consumedProperties() {
        return Collections.singleton(context.properties().get("describeContents"));
      }

      @Override
      public Collection<String> additionalImports() {
        return getImports();
      }

      @Override
      public Collection<String> additionalInterfaces() {
        return Collections.singleton("Parcelable");
      }

      @Override
      public Collection<String> additionalCode() {
        Map<String, ExecutableElement> properties = new HashMap<String, ExecutableElement>(context.properties());
        properties.remove("describeContents");
        return Collections.singleton(getParcelableCode(className, properties));
      }
    };
  }

  private Collection<String> getImports() {
    return Arrays.asList("android.os.Parcel", "java.lang.ClassLoader", "android.os.Parcelable");
  }

  private String getParcelableCode(String className, Map<String, ExecutableElement> properties) {
    CodeBuilder code = new CodeBuilder();
    code.writeLn("  public static final Parcelable.Creator<" + className + "> CREATOR = new Parcelable.Creator<" + className + ">() {");
    code.writeLn("    @Override public " + className + " createFromParcel(android.os.Parcel in) {");
    code.writeLn("      return new " + className + "(in);");
    code.writeLn("    }");
    code.writeLn("");
    code.writeLn("    @Override public " + className + "[] newArray(int size) {");
    code.writeLn("      return new " + className + "[size];");
    code.writeLn("    }");
    code.writeLn("  };");
    code.writeLn("");
    code.writeLn("  private final static ClassLoader CL = " + className + ".class.getClassLoader();");
    code.writeLn("");
    code.writeLn("  private " + className + "(Parcel in) {");
    List<ExecutableElement> props = new ArrayList<ExecutableElement>(properties.values());
    for (int i = 0; i < props.size(); i++) {
      ExecutableElement prop = props.get(i);
      if (isParcelableType(prop)) {
        code.writeLn("    " + prop.getSimpleName() + " = (" + getCastType(prop) + ") in.readValue(CL);");
      } else {
        code.writeLn("    // " + prop.getSimpleName() + " not a parcelable type.");
      }
    }
    code.writeLn("  }");
    code.writeLn("");
    code.writeLn("  @Override public void writeToParcel(Parcel dest, int flags) {");
    for (ExecutableElement prop : properties.values()) {
      if (isParcelableType(prop)) {
        code.writeLn("    dest.writeValue(" + prop.getSimpleName() + ");");
      }
    }
    code.writeLn("  }");
    code.writeLn("");
    code.writeLn("  @Override public int describeContents() {");
    code.writeLn("    return 0;");
    code.writeLn("  }");
    return code.toString();
  }

  private boolean isParcelableType(ExecutableElement prop) {
    if (primitive(prop))
      return true;

    // special case for strings
    if (getCastType(prop).equals("java.lang.String"))
      return true;

    TypeElement typeElement = MoreTypes.asTypeElement(prop.getReturnType());
    for (TypeMirror iface : typeElement.getInterfaces()) {
      String typeName = context.processingEnvironment().getTypeUtils().asElement(iface).getSimpleName().toString();
      getMessager(context).printMessage(Diagnostic.Kind.NOTE, "Checking type: " + typeName);
      if (typeName.equals("android.os.Parcelable")) {
        return true;
      }
    }

    return false;
  }

  class CodeBuilder {

    private StringBuilder code = new StringBuilder();

    CodeBuilder writeLn(String code) {
      this.code.append(code).append("\n");
      return this;
    }

    CodeBuilder write(String code) {
      this.code.append(code);
      return this;
    }

    @Override
    public String toString() {
      return code.toString();
    }
  }

  public String getCastType(ExecutableElement prop) {
    return primitive(prop) ? box(prop.getReturnType().getKind()) : prop.getReturnType().toString();
  }

  private String box(TypeKind kind) {
    switch (kind) {
      case BOOLEAN:
        return "Boolean";
      case BYTE:
        return "Byte";
      case SHORT:
        return "Short";
      case INT:
        return "Integer";
      case LONG:
        return "Long";
      case CHAR:
        return "Character";
      case FLOAT:
        return "Float";
      case DOUBLE:
        return "Double";
      default:
        throw new RuntimeException("Unknown primitive of kind " + kind);
    }
  }

  public boolean primitive(ExecutableElement el) {
    return el.getReturnType().getKind().isPrimitive();
  }
}
