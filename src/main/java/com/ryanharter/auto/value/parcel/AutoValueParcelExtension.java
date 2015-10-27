package com.ryanharter.auto.value.parcel;

import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(AutoValueExtension.class)
public class AutoValueParcelExtension extends AutoValueExtension {

  public static final class Property {
    String name;
    ExecutableElement element;
    TypeName type;
    ImmutableSet<String> annotations;

    public Property() {}

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
    List<Property> properties = readProperties(context.properties());
    validateProperties(context.processingEnvironment(), properties);

    TypeName type = ClassName.bestGuess(className);

    FieldSpec classLoader = FieldSpec
        .builder(ClassName.get(ClassLoader.class), "CL", Modifier.PRIVATE, Modifier.FINAL,
            Modifier.STATIC)
        .initializer("$T.class.getClassLoader()", type)
        .build();

    MethodSpec constructor = generateConstructor(properties);
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

  private List<Property> readProperties(Map<String, ExecutableElement> properties) {
    List<Property> values = new LinkedList<Property>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      values.add(new Property(entry.getKey(), entry.getValue()));
    }
    return values;
  }

  private void validateProperties(ProcessingEnvironment env,
                                  List<Property> properties) {
    Types typeUtils = env.getTypeUtils();
    Elements elementUtils = env.getElementUtils();
    TypeMirror list = typeUtils.getDeclaredType(
        elementUtils.getTypeElement(List.class.getName()),
        typeUtils.getWildcardType(null, null)
    );
    TypeMirror parcelable = env.getElementUtils().getTypeElement("android.os.Parcelable").asType();
    TypeMirror serializable = TypeSimplifier.typeFromClass(env.getTypeUtils(),
        env.getElementUtils(), Serializable.class);
    for (Property property : properties) {
      TypeMirror type = property.element.getReturnType();
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
            property.name + " is not primitive, Parcelable, or Serializable.", property.element);
        throw new AutoValueParcelException();
      }
    }
  }

  private static final List<String> METHOD_EXCLUDES =
      Arrays.asList("describeContents", "toBuilder");

  private Map<String, TypeName> convertPropertiesToTypes(
      Map<String, ExecutableElement> properties) {
    Map<String, TypeName> types = new LinkedHashMap<String, TypeName>();
    for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
      if (METHOD_EXCLUDES.contains(entry.getKey())) continue;
      types.put(entry.getKey(), TypeName.get(entry.getValue().getReturnType()));
    }
    return types;
  }

  MethodSpec generateConstructor(List<Property> properties) {
    List<ParameterSpec> params = Lists.newArrayList();
    for (Property property : properties) {
      params.add(ParameterSpec.builder(property.type, property.name).build());
    }

    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addParameters(params);

    StringBuilder superFormat = new StringBuilder("super(");
    List<TypeName> args = new LinkedList<TypeName>();
    for (int i = 0, n = properties.size(); i < n; i++) {
      args.add(properties.get(i).type);
      superFormat.append("$N");
      if (i < n - 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), args.toArray());

    return builder.build();
  }

  MethodSpec generateParcelConstructor(List<Property> properties, FieldSpec classLoader) {
    ParameterSpec in = ParameterSpec.builder(ClassName.get("android.os", "Parcel"), "in").build();
    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PRIVATE)
        .addParameter(in);

    CodeBlock.Builder content = CodeBlock.builder();
    content.add("super(\n");
    content.indent();
    for (int i = 0, n = properties.size(); i < n; i++) {
      content.add(Parcelables.readValue(properties.get(i), in, classLoader));
      if (i < n - 1) content.add(",");
      content.add("\n");
    }
    content.unindent();
    content.add(");");

    return builder.addCode(content.build()).build();
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

  MethodSpec generateWriteToParcel(List<Property> properties) {
    ParameterSpec dest = ParameterSpec
        .builder(ClassName.get("android.os", "Parcel"), "dest")
        .build();
    MethodSpec.Builder builder = MethodSpec.methodBuilder("writeToParcel")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(dest)
        .addParameter(int.class, "flags");

    for (Property p : properties) {
      builder.addCode(Parcelables.writeValue(p, dest));
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
