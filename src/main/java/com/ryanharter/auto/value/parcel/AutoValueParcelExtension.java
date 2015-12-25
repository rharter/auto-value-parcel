package com.ryanharter.auto.value.parcel;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(AutoValueExtension.class)
public final class AutoValueParcelExtension extends AutoValueExtension {

  static final class Property {
    String name;
    ExecutableElement element;
    TypeName type;
    ImmutableSet<String> annotations;

    public Property(String name, ExecutableElement element) {
      this.name = name;
      this.element = element;
      type = TypeName.get(element.getReturnType());
      annotations = buildAnnotations(element);
    }

    public Boolean nullable() {
      return annotations.contains("Nullable");
    }

    private ImmutableSet<String> buildAnnotations(ExecutableElement element) {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
        builder.add(annotation.getAnnotationType().asElement().getSimpleName().toString());
      }
      return builder.build();
    }
  }

  @Override
  public boolean applicable(Context context) {
    TypeMirror parcelable = context.processingEnvironment().getElementUtils()
        .getTypeElement("android.os.Parcelable").asType();
    TypeMirror autoValueClass = context.autoValueClass().asType();
    return TypeSimplifier.isClassOfType(context.processingEnvironment().getTypeUtils(), parcelable,
        autoValueClass);
  }

  @Override
  public Set<String> consumeProperties(Context context) {
    return Sets.newHashSet("describeContents", "writeToParcel");
  }

  @Override
  public boolean mustBeFinal(Context context) {
    return true;
  }

  @Override
  public String generateClass(Context context, String className, String classToExtend,
                              boolean isFinal) {
    ImmutableList<Property> properties = readProperties(context.properties());
    validateProperties(context.processingEnvironment(), properties);

    TypeName type = ClassName.bestGuess(className);

    FieldSpec classLoader = FieldSpec
        .builder(ClassName.get(ClassLoader.class), "CL", Modifier.FINAL, Modifier.STATIC)
        .initializer("$T.class.getClassLoader()", type)
        .build();

    MethodSpec constructor = generateConstructor(properties);
    MethodSpec writeToParcel = generateWriteToParcel(context.processingEnvironment(), properties);
    MethodSpec describeContents = generateDescribeContents();
    FieldSpec creator =
        generateCreator(context.processingEnvironment(), properties, type, classLoader);

    TypeSpec subclass = TypeSpec.classBuilder(className)
        .addModifiers(Modifier.FINAL)
        .superclass(TypeVariableName.get(classToExtend))
        .addMethod(constructor)
        .addField(classLoader)
        .addField(creator)
        .addMethod(writeToParcel)
        .addMethod(describeContents)
        .build();

    JavaFile javaFile = JavaFile.builder(context.packageName(), subclass).build();
    return javaFile.toString();
  }

  private ImmutableList<Property> readProperties(Map<String, ExecutableElement> properties) {
    ImmutableList.Builder<Property> values = ImmutableList.builder();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      values.add(new Property(entry.getKey(), entry.getValue()));
    }
    return values.build();
  }

  private void validateProperties(ProcessingEnvironment env, List<Property> properties) {
    Types typeUtils = env.getTypeUtils();
    for (Property property : properties) {
      TypeMirror type = property.element.getReturnType();
      if (type.getKind() == TypeKind.ARRAY) {
        ArrayType aType = (ArrayType) type;
        type = aType.getComponentType();
      }
      TypeElement element = (TypeElement) typeUtils.asElement(type);
      if ((element == null || !Parcelables.isValidType(typeUtils, element)) &&
          !Parcelables.isValidType(TypeName.get(type))){
        env.getMessager().printMessage(Diagnostic.Kind.ERROR, "AutoValue property " +
            property.name + " is not a supported Parcelable type.", property.element);
        throw new AutoValueParcelException();
      }
    }
  }

  MethodSpec generateConstructor(List<Property> properties) {
    List<ParameterSpec> params = Lists.newArrayListWithCapacity(properties.size());
    for (Property property : properties) {
      params.add(ParameterSpec.builder(property.type, property.name).build());
    }

    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addParameters(params);

    StringBuilder superFormat = new StringBuilder("super(");
    List<ParameterSpec> args = new ArrayList<ParameterSpec>();
    for (int i = 0, n = params.size(); i < n; i++) {
      args.add(params.get(i));
      superFormat.append("$N");
      if (i < n - 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), args.toArray());

    return builder.build();
  }

  FieldSpec generateCreator(ProcessingEnvironment env, List<Property> properties, TypeName type,
      FieldSpec classLoader) {
    ClassName creator = ClassName.bestGuess("android.os.Parcelable.Creator");
    TypeName creatorOfClass = ParameterizedTypeName.get(creator, type);

    Types typeUtils = env.getTypeUtils();
    CodeBlock.Builder ctorCall = CodeBlock.builder();
    ctorCall.add("return new $T(\n", type);
    ctorCall.indent().indent();
    for (int i = 0, n = properties.size(); i < n; i++) {
      ctorCall.add(Parcelables.readValue(typeUtils, properties.get(i), classLoader));
      if (i < n - 1) ctorCall.add(",");
      ctorCall.add("\n");
    }
    ctorCall.unindent().unindent();
    ctorCall.add(");\n");

    TypeSpec creatorImpl = TypeSpec.anonymousClassBuilder("")
        .superclass(creatorOfClass)
        .addMethod(MethodSpec.methodBuilder("createFromParcel")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(type)
            .addParameter(ClassName.bestGuess("android.os.Parcel"), "in")
            .addCode(ctorCall.build())
            .build())
        .addMethod(MethodSpec.methodBuilder("newArray")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ArrayTypeName.of(type))
            .addParameter(int.class, "size")
            .addStatement("return new $T[size]", type)
            .build())
        .build();

    return FieldSpec
        .builder(creatorOfClass, "CREATOR", Modifier.PUBLIC,
            Modifier.FINAL, Modifier.STATIC)
        .initializer(creatorImpl.toString())
        .build();
  }

  MethodSpec generateWriteToParcel(ProcessingEnvironment env, List<Property> properties) {
    ParameterSpec dest = ParameterSpec
        .builder(ClassName.get("android.os", "Parcel"), "dest")
        .build();
    MethodSpec.Builder builder = MethodSpec.methodBuilder("writeToParcel")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(dest)
        .addParameter(int.class, "flags");

    Types typeUtils = env.getTypeUtils();
    for (Property p : properties) {
      builder.addCode(Parcelables.writeValue(typeUtils, p, dest));
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
