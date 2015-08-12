package com.ryanharter.autoparcel;

import autovalue.shaded.com.google.common.common.collect.Lists;
import autovalue.shaded.com.google.common.common.collect.Sets;
import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValueExtension;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.Serializable;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.*;

public class AutoParcelExtension implements AutoValueExtension {

  Messager getMessager(Context context) {
    return context.processingEnvironment().getMessager();
  }

  @Override
  public boolean applicable(Context context) {
    TypeElement parcelable = context.processingEnvironment().getElementUtils()
        .getTypeElement("android.os.Parcelable");
    TypeMirror autoValueClass = context.autoValueClass().asType();
    return TypeSimplifier.isClassOfType(context.processingEnvironment().getTypeUtils(),
        parcelable.asType(), autoValueClass);
  }

  @Override
  public boolean mustBeAtEnd(Context context) {
    return true;
  }

  @Override
  public String generateClass(final Context context, final String className,
                              final String classToExtend, boolean isFinal) {
    return getParcelableCode(context, className, classToExtend, context.properties());
  }

  String getParcelableCode(Context context, String className, String classToExtend,
                           Map<String, ExecutableElement> properties) {
    validateProperties(context.processingEnvironment(), properties);

    Map<String, TypeName> types = convertPropertiesToTypes(properties);

    FieldSpec classLoader = FieldSpec
        .builder(ClassName.get(ClassLoader.class), "CL", Modifier.PRIVATE, Modifier.FINAL,
            Modifier.STATIC)
        .initializer("$N.class.getClassLoader()", className)
        .build();

    MethodSpec constructor = generateConstructor(types);
    MethodSpec parcelConstructor = generateParcelConstructor(types, classLoader);
    MethodSpec writeToParcel = generateWriteToParcel(types);
    MethodSpec describeContents = generateDescribeContents();
    FieldSpec creator = generateCreator(className, classLoader);

    TypeSpec subclass = TypeSpec.classBuilder(className)
        .addModifiers(Modifier.FINAL)
        .superclass(TypeVariableName.get(classToExtend))
        .addMethod(constructor)
        .addField(classLoader)
        .addField(creator)
        .addMethod(writeToParcel)
        .addMethod(describeContents)
        .addMethod(parcelConstructor)
        .build();

    JavaFile javaFile = JavaFile.builder(context.packageName(), subclass).build();
    return javaFile.toString();
  }

  private void validateProperties(ProcessingEnvironment env,
                                  Map<String, ExecutableElement> properties) {
    TypeElement parcelable = env.getElementUtils().getTypeElement("android.os.Parcelable");
    TypeMirror serializable = TypeSimplifier.typeFromClass(env.getTypeUtils(),
        env.getElementUtils(), Serializable.class);
    for (ExecutableElement name : properties.values()) {
      TypeMirror type = name.getReturnType();
      if (!TypeName.get(type).isPrimitive() &&
          !TypeSimplifier.isClassOfType(env.getTypeUtils(), parcelable.asType(), type) &&
          !TypeSimplifier.isClassOfType(env.getTypeUtils(), serializable, type)) {
        env.getMessager().printMessage(Diagnostic.Kind.ERROR, "AutoValue property " +
            name.getSimpleName() + " is not primitive, Parcelable or Serializable.", name);
        throw new AutoParcelException();
      }
    }
  }

  private Map<String, TypeName> convertPropertiesToTypes(
      Map<String, ExecutableElement> properties) {
    Map<String, TypeName> types = new LinkedHashMap<String, TypeName>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      if ("describeContents".equals(entry.getKey())) continue;
      types.put(entry.getKey(), TypeName.get(entry.getValue().getReturnType()));
    }
    return types;
  }

  MethodSpec generateConstructor(Map<String, TypeName> properties) {
    List<ParameterSpec> params = Lists.newArrayList();
    for (Map.Entry<String, TypeName> entry : properties.entrySet()) {
      params.add(ParameterSpec.builder(entry.getValue(), entry.getKey()).build());
    }

    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addParameters(params);

    StringBuilder superFormat = new StringBuilder("super(");
    for (int i = properties.size(); i > 0; i--) {
      superFormat.append("$N");
      if (i > 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), properties.keySet().toArray());

    return builder.build();
  }

  MethodSpec generateParcelConstructor(Map<String, TypeName> types, FieldSpec classLoader) {
    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PRIVATE)
        .addParameter(ClassName.bestGuess("android.os.Parcel"), "in");

    List<TypeName> args = new ArrayList<TypeName>(types.size());
    List<TypeName> typeNames = new ArrayList<TypeName>(types.values());
    for (TypeName name : typeNames) {
      if (name.isPrimitive()) {
        args.add(name.box());
      } else {
        args.add(name);
      }
    }

    StringBuilder superFormat = new StringBuilder("this(");
    for (int i = args.size(); i > 0; i--) {
      superFormat.append("($T) in.readValue(" + classLoader.name + ")");
      if (i > 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), args.toArray());
    return builder.build();
  }

  FieldSpec generateCreator(String cls, FieldSpec classLoader) {
    ClassName className = ClassName.bestGuess(cls);
    ClassName creator = ClassName.bestGuess("android.os.Parcelable.Creator");
    TypeName creatorOfClass = ParameterizedTypeName.get(creator, className);

    TypeSpec creatorImpl = TypeSpec.anonymousClassBuilder("")
        .superclass(creatorOfClass)
        .addMethod(MethodSpec.methodBuilder("createFromParcel")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(className)
            .addParameter(ClassName.bestGuess("android.os.Parcel"), "in")
            .addStatement("return new $T(in)", className)
            .build())
        .addMethod(MethodSpec.methodBuilder("newArray")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ArrayTypeName.of(className))
            .addParameter(int.class, "size")
            .addStatement("return new $T[size]", className)
            .build())
        .build();

    return FieldSpec
        .builder(creatorOfClass, "CREATOR", Modifier.PRIVATE,
            Modifier.FINAL, Modifier.STATIC)
        .initializer(creatorImpl.toString())
        .build();
  }

  MethodSpec generateWriteToParcel(Map<String, TypeName> types) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("writeToParcel")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ClassName.bestGuess("android.os.Parcel"), "dest")
        .addParameter(int.class, "flags");

    for (String name : types.keySet()) {
      builder.addStatement("dest.writeValue($N())", name);
    }

    return builder.build();
  }

  MethodSpec generateDescribeContents() {
    return MethodSpec.methodBuilder("describeContents")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(int.class)
        .addStatement("return 0")
        .build();
  }

  private boolean isParcelableType(Context context, ExecutableElement prop) {
    if (primitive(prop))
      return true;

    // special case for strings
    if (getCastType(prop).equals("java.lang.String"))
      return true;

    TypeElement typeElement = MoreTypes.asTypeElement(prop.getReturnType());
    for (TypeMirror iface : typeElement.getInterfaces()) {
      String typeName = context.processingEnvironment().getTypeUtils().asElement(iface)
          .getSimpleName().toString();
      getMessager(context).printMessage(Diagnostic.Kind.NOTE, "Checking type: " + typeName);
      if (typeName.equals("android.os.Parcelable")) {
        return true;
      }
    }

    return false;
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
