package com.ryanharter.auto.value.parcel;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(AutoValueExtension.class)
public final class AutoValueParcelExtension extends AutoValueExtension {

  static final class Property {
    final String methodName;
    final String humanName;
    final ExecutableElement element;
    final TypeName type;
    final ImmutableSet<String> annotations;
    TypeMirror typeAdapter;

    public Property(String humanName, ExecutableElement element) {
      this.methodName = element.getSimpleName().toString();
      this.humanName = humanName;
      this.element = element;
      type = TypeName.get(element.getReturnType());
      annotations = buildAnnotations(element);

      ParcelAdapter parcelAdapter = element.getAnnotation(ParcelAdapter.class);
      if (parcelAdapter != null) {
        try {
          parcelAdapter.value();
        } catch (MirroredTypeException e) {
          typeAdapter = e.getTypeMirror();
        }
      }
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
    ProcessingEnvironment env = context.processingEnvironment();

    ImmutableList<Property> properties = readProperties(context.properties());
    validateProperties(env, properties);

    TypeName type = ClassName.bestGuess(className);
    TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
        .addModifiers(Modifier.FINAL)
        .superclass(TypeVariableName.get(classToExtend))
        .addMethod(generateConstructor(properties))
        .addField(generateCreator(env, properties, type))
        .addMethod(generateWriteToParcel(env, properties));

    if (needsContentDescriptor(context)) {
      subclass.addMethod(generateDescribeContents());
    }

    JavaFile javaFile = JavaFile.builder(context.packageName(), subclass.build()).build();
    return javaFile.toString();
  }

  private static boolean needsContentDescriptor(Context context) {
    ProcessingEnvironment env = context.processingEnvironment();
    for (ExecutableElement element : MoreElements.getLocalAndInheritedMethods(
        context.autoValueClass(), env.getElementUtils())) {
      if (element.getSimpleName().contentEquals("describeContents")
          && MoreTypes.isTypeOf(int.class, element.getReturnType())
          && element.getParameters().isEmpty()
          && !element.getModifiers().contains(Modifier.ABSTRACT)) {
        return false;
      }
    }
    return true;
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
      if (property.typeAdapter != null) {
        continue;
      }
      TypeMirror type = property.element.getReturnType();
      if (type.getKind() == TypeKind.ARRAY) {
        ArrayType aType = (ArrayType) type;
        type = aType.getComponentType();
      }
      TypeElement element = (TypeElement) typeUtils.asElement(type);
      if ((element == null || !Parcelables.isValidType(typeUtils, element)) &&
          !Parcelables.isValidType(TypeName.get(type))){
        env.getMessager().printMessage(Diagnostic.Kind.ERROR, "AutoValue property " +
            property.methodName + " is not a supported Parcelable type.", property.element);
        throw new AutoValueParcelException();
      }
    }
  }

  MethodSpec generateConstructor(List<Property> properties) {
    List<ParameterSpec> params = Lists.newArrayListWithCapacity(properties.size());
    for (Property property : properties) {
      params.add(ParameterSpec.builder(property.type, property.humanName).build());
    }

    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addParameters(params);

    StringBuilder superFormat = new StringBuilder("super(");
    List<ParameterSpec> args = new ArrayList<>();
    for (int i = 0, n = params.size(); i < n; i++) {
      args.add(params.get(i));
      superFormat.append("$N");
      if (i < n - 1) superFormat.append(", ");
    }
    superFormat.append(")");
    builder.addStatement(superFormat.toString(), args.toArray());

    return builder.build();
  }

  FieldSpec generateCreator(ProcessingEnvironment env, List<Property> properties, TypeName type) {
    ClassName creator = ClassName.bestGuess("android.os.Parcelable.Creator");
    TypeName creatorOfClass = ParameterizedTypeName.get(creator, type);

    ImmutableMap<Property, FieldSpec> typeAdapters = getTypeAdapters(properties);
    Types typeUtils = env.getTypeUtils();
    CodeBlock.Builder ctorCall = CodeBlock.builder();
    ctorCall.add("return new $T(\n", type);
    ctorCall.indent().indent();
    boolean requiresClassLoader = false;
    for (int i = 0, n = properties.size(); i < n; i++) {
      Property property = properties.get(i);
      if (typeAdapters.containsKey(property)) {
        ctorCall.add("$N.fromParcel(in)", typeAdapters.get(property));
      } else {
        requiresClassLoader |= Parcelables.appendReadValue(ctorCall, property, typeUtils);
      }

      if (i < n - 1) ctorCall.add(",");
      ctorCall.add("\n");
    }
    ctorCall.unindent().unindent();
    ctorCall.add(");\n");

    MethodSpec.Builder createFromParcel = MethodSpec.methodBuilder("createFromParcel")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(type)
        .addParameter(ClassName.bestGuess("android.os.Parcel"), "in");
    if (requiresClassLoader) {
      createFromParcel.addStatement("$T cl = $T.class.getClassLoader()", ClassLoader.class, type);
    }
    for (FieldSpec field : typeAdapters.values()) {
      createFromParcel.addStatement("$T $N = new $T()", field.type, field, field.type);
    }
    createFromParcel.addCode(ctorCall.build());

    TypeSpec creatorImpl = TypeSpec.anonymousClassBuilder("")
        .superclass(creatorOfClass)
        .addMethod(createFromParcel
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
        .initializer("$L", creatorImpl)
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

    ImmutableMap<Property, FieldSpec> typeAdapters = getTypeAdapters(properties);
    for (FieldSpec field : typeAdapters.values()) {
      builder.addStatement("$T $N = new $T()", field.type, field, field.type);
    }

    Types typeUtils = env.getTypeUtils();
    for (Property p : properties) {
      if (typeAdapters.containsKey(p)) {
        builder.addStatement("$N.toParcel($N(), $N)", typeAdapters.get(p), p.methodName, dest);
      } else {
        builder.addCode(Parcelables.writeValue(typeUtils, p, dest));
      }
    }

    return builder.build();
  }

  private ImmutableMap<Property, FieldSpec> getTypeAdapters(List<Property> properties) {
    Map<Property, FieldSpec> typeAdapters = new HashMap<>();
    for (Property property : properties) {
      if (property.typeAdapter != null && !typeAdapters.containsKey(property)) {
        ClassName typeName = (ClassName) TypeName.get(property.typeAdapter);
        String name = Character.toLowerCase(typeName.simpleName().charAt(0))
            + typeName.simpleName().substring(1);

        typeAdapters.put(property, FieldSpec.builder(
            typeName, NameAllocator.toJavaIdentifier(name)).build());
      }
    }
    return ImmutableMap.copyOf(typeAdapters);
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
