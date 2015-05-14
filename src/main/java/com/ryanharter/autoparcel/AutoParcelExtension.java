package com.ryanharter.autoparcel;

import com.google.auto.value.AutoValueExtension;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.*;

/**
 * Created by rharter on 5/1/15.
 */
public class AutoParcelExtension implements AutoValueExtension {

  Messager getMessager(Context context) {
    return context.processingEnvironment().getMessager();
  }

  @Override
  public boolean applicable(Context context) {
    getMessager(context).printMessage(Diagnostic.Kind.NOTE, "Checking applicability: " + context.autoValueClass(), context.autoValueClass());

    TypeElement parcelable = context.processingEnvironment().getElementUtils().getTypeElement("android.os.Parcelable");
    return parcelable != null &&
        context.processingEnvironment().getTypeUtils().isAssignable(context.autoValueClass().asType(), parcelable.asType());
  }

  @Override
  public AutoValueExtension.GeneratedClass generateClass(final Context context, final String className, final String classToExtend, String classToImplement) {
    getMessager(context).printMessage(Diagnostic.Kind.NOTE, "Generating class: " + context.autoValueClass(), context.autoValueClass());
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
        return Collections.singleton(getParcelableCode(className, context.properties()));
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
      if (prop.getSimpleName().equals("describeContents")) {
        continue;
      }
      code.write("    " + prop + " = (" + getCastType(prop) + ") in.readValue(CL);");
      if (i == props.size() - 1) {
        code.write("\n");
      } else {
        code.writeLn(",");
      }
    }
    code.writeLn("  }");
    code.writeLn("");
    code.writeLn("  @Override public void writeToParcel(Parcel dest, int flags) {");
    for (String prop : properties.keySet()) {
      code.writeLn("  dest.writeValue(" + prop + ");");
    }
    code.writeLn("  }");
    code.writeLn("");
    code.writeLn("  @Override public int describeContents() {");
    code.writeLn("    return 0;");
    code.writeLn("  }");
    return code.toString();
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
    return primitive(prop) ? box(prop.getReturnType().getKind()) : prop.asType().toString();
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
