package com.ryanharter.auto.value.parcel;

import com.google.auto.value.AutoValueExtension;
import com.google.common.collect.Lists;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

public class AutoValueParcelExtension implements AutoValueExtension {

  @Override
  public boolean applicable(Context context) {
    TypeMirror parcelable = context.processingEnvironment().getElementUtils()
        .getTypeElement("android.os.Parcelable").asType();
    TypeMirror autoValueClass = context.autoValueClass().asType();
    return TypeSimplifier.isClassOfType(context.processingEnvironment().getTypeUtils(), parcelable,
        autoValueClass);
  }

  @Override
  public boolean mustBeAtEnd(Context context) {
    return true;
  }

  @Override
  public String generateClass(Context context, String className, String classToExtend,
                              boolean isFinal) {
    return getParcelableCode(context, className, classToExtend, context.properties());
  }

  String getParcelableCode(Context context, String className, String classToExtend,
                           Map<String, ExecutableElement> properties) {
    validateProperties(context.processingEnvironment(), properties);

    Map<String, TypeName> types = convertPropertiesToTypes(properties);
    TypeName type = ClassName.bestGuess(className);

    FieldSpec classLoader = FieldSpec
        .builder(ClassName.get(ClassLoader.class), "CL", Modifier.PRIVATE, Modifier.FINAL,
            Modifier.STATIC)
        .initializer("$T.class.getClassLoader()", type)
        .build();

    MethodSpec constructor = generateConstructor(types);
    MethodSpec parcelConstructor = generateParcelConstructor(types, classLoader);
    MethodSpec writeToParcel = generateWriteToParcel(types);
    MethodSpec describeContents = generateDescribeContents();
    FieldSpec creator = generateCreator(className);

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
    Types typeUtils = env.getTypeUtils();
    Elements elementUtils = env.getElementUtils();
    TypeMirror list = typeUtils.getDeclaredType(
        elementUtils.getTypeElement(List.class.getName()),
        typeUtils.getWildcardType(null, null)
    );
    TypeMirror parcelable = env.getElementUtils().getTypeElement("android.os.Parcelable").asType();
    TypeMirror serializable = TypeSimplifier.typeFromClass(env.getTypeUtils(),
        env.getElementUtils(), Serializable.class);
    for (ExecutableElement name : properties.values()) {
      TypeMirror type = name.getReturnType();
      if (type.getKind() == TypeKind.DECLARED && typeUtils.isSubtype(type, list)) {
        DeclaredType dType = (DeclaredType) type;
        List<? extends TypeMirror> types = dType.getTypeArguments();
        if (!types.isEmpty()) {
          type = types.get(0);
        }
      } else if (type.getKind() == TypeKind.ARRAY) {
        ArrayType aType = (ArrayType) type;
        type = aType.getComponentType();
      }

      if (!TypeName.get(type).isPrimitive() &&
          !TypeSimplifier.isClassOfType(env.getTypeUtils(), parcelable, type) &&
          !TypeSimplifier.isClassOfType(env.getTypeUtils(), serializable, type)) {
        env.getMessager().printMessage(Diagnostic.Kind.ERROR, "AutoValue property " +
            name.getSimpleName() + " is not primitive, Parcelable, or Serializable.", name);
        throw new AutoValueParcelException();
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
      superFormat.append("($T) in.readValue(").append(classLoader.name).append(")");
      if (i > 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), args.toArray());
    return builder.build();
  }

  FieldSpec generateCreator(String cls) {
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
        .builder(creatorOfClass, "CREATOR", Modifier.PUBLIC,
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
}
