package com.ryanharter.autoparcels;

import com.google.auto.value.AutoValueExtension;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;

/**
 * Created by rharter on 5/1/15.
 */
public class AutoParcelsExtension implements AutoValueExtension {

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
        return String.format("package %s;" +
            "" +
            "abstract class %s extends %s {" +
            "" +
            "  public static final android.os.Parcelable.Creator<%s> CREATOR = new android.os.Parcelable.Creator<%s>() {" +
            "    @Override public %s createFromParcel(android.os.Parcel in) {" +
            "      return new %s(in);" +
            "    }" +
            "  }" +
            "  @Override public " +
            "}", context.packageName(), className, classToExtend, className, className, className, className);
      }

      @Override
      public Collection<ExecutableElement> consumedProperties() {
        return Arrays.asList(context.properties().get("describeContents"));
      }
    };
  }
}
