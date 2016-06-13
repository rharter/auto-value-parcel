package com.ryanharter.auto.value.parcel;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.squareup.javapoet.AnnotationSpec;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.element.Modifier.FINAL;

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
    TypeMirror autoValueClass = context.autoValueClass().asType();
    // Disallow manual implementation of the CREATOR instance
    VariableElement creator = findCreator(context);
    if (creator != null) {
      context.processingEnvironment().getMessager().printMessage(Diagnostic.Kind.ERROR,
          "Manual implementation of a static Parcelable.Creator<T> CREATOR field found when processing "
              + autoValueClass.toString() + ". Remove this so auto-value-parcel can automatically generate the "
              + "implementation for you.", creator);
    }
    // Disallow manual implementation of writeToParcel
    ExecutableElement writeToParcel = findWriteToParcel(context);
    if (writeToParcel != null) {
      context.processingEnvironment().getMessager().printMessage(Diagnostic.Kind.ERROR,
          "Manual implementation of Parcelable#writeToParcel(Parcel,int) found when processing "
              + autoValueClass.toString() + ". Remove this so auto-value-parcel can automatically generate the "
              + "implementation for you.", writeToParcel);
    }
    TypeMirror parcelable = context.processingEnvironment().getElementUtils()
        .getTypeElement("android.os.Parcelable").asType();
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

    ImmutableMap<TypeMirror, FieldSpec> typeAdapters = getTypeAdapters(properties);

    TypeName type = ClassName.get(context.packageName(), className);
    TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
        .addModifiers(FINAL)
        .superclass(ClassName.get(context.packageName(), classToExtend))
        .addMethod(generateConstructor(properties))
        .addMethod(generateWriteToParcel(env, properties, typeAdapters));

    if (!typeAdapters.isEmpty()) {
      for (FieldSpec field : typeAdapters.values()) {
        subclass.addField(field);
      }
    }

    subclass.addField(generateCreator(env, properties, type, typeAdapters));

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
          && !element.getModifiers().contains(ABSTRACT)) {
        return false;
      }
    }
    return true;
  }

  private static ExecutableElement findWriteToParcel(Context context) {
    ProcessingEnvironment env = context.processingEnvironment();
    TypeMirror parcel = env.getElementUtils().getTypeElement("android.os.Parcel").asType();
    for (ExecutableElement element : MoreElements.getLocalAndInheritedMethods(
        context.autoValueClass(), env.getElementUtils())) {
      if (element.getSimpleName().contentEquals("writeToParcel")
          && MoreTypes.isTypeOf(void.class, element.getReturnType())
          && !element.getModifiers().contains(ABSTRACT)) {
        List<? extends VariableElement> parameters = element.getParameters();
        if (parameters.size() == 2
            && env.getTypeUtils().isSameType(parcel, parameters.get(0).asType())
            && MoreTypes.isTypeOf(int.class, parameters.get(1).asType())) {
          return element;
        }
      }
    }
    return null;
  }

  private static VariableElement findCreator(Context context) {
    ProcessingEnvironment env = context.processingEnvironment();
    Types typeUtils = env.getTypeUtils();
    Elements elementUtils = env.getElementUtils();
    TypeMirror creatorType = typeUtils.erasure(elementUtils.getTypeElement("android.os.Parcelable.Creator").asType());
    List<? extends Element> members = env.getElementUtils().getAllMembers(context.autoValueClass());
    for (VariableElement field : ElementFilter.fieldsIn(members)) {
      if (field.getSimpleName().contentEquals("CREATOR")
          && typeUtils.isSameType(creatorType, typeUtils.erasure(field.asType()))
          && field.getModifiers().contains(STATIC)) {
        return field;
      }
    }
    return null;
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

  FieldSpec generateCreator(ProcessingEnvironment env, List<Property> properties, TypeName type,
      Map<TypeMirror, FieldSpec> typeAdapters) {
    ClassName creator = ClassName.bestGuess("android.os.Parcelable.Creator");
    TypeName creatorOfClass = ParameterizedTypeName.get(creator, type);

    Types typeUtils = env.getTypeUtils();
    CodeBlock.Builder ctorCall = CodeBlock.builder();
    ctorCall.add("return new $T(\n", type);
    ctorCall.indent().indent();
    boolean requiresClassLoader = false;
    boolean requiresSuppressWarnings = false;
    for (int i = 0, n = properties.size(); i < n; i++) {
      Property property = properties.get(i);
      if (property.typeAdapter != null && typeAdapters.containsKey(property.typeAdapter)) {
        Parcelables.readValueWithTypeAdapter(ctorCall, property,
            typeAdapters.get(property.typeAdapter));
      } else {
        final TypeName typeName = Parcelables.getTypeNameFromProperty(property, typeUtils);
        requiresClassLoader |= Parcelables.isTypeRequiresClassLoader(typeName);
        requiresSuppressWarnings |= Parcelables.isTypeRequiresSuppressWarnings(typeName);
        Parcelables.readValue(ctorCall, property, typeName);
      }

      if (i < n - 1) ctorCall.add(",");
      ctorCall.add("\n");
    }
    ctorCall.unindent().unindent();
    ctorCall.add(");\n");

    MethodSpec.Builder createFromParcel = MethodSpec.methodBuilder("createFromParcel")
        .addAnnotation(Override.class);
    if (requiresSuppressWarnings) {
      createFromParcel.addAnnotation(createSuppressUncheckedWarningAnnotation());
    }
    createFromParcel
        .addModifiers(PUBLIC)
        .returns(type)
        .addParameter(ClassName.bestGuess("android.os.Parcel"), "in");
    if (requiresClassLoader) {
      createFromParcel.addStatement("$T cl = $T.class.getClassLoader()", ClassLoader.class, type);
    }
    createFromParcel.addCode(ctorCall.build());

    TypeSpec creatorImpl = TypeSpec.anonymousClassBuilder("")
        .superclass(creatorOfClass)
        .addMethod(createFromParcel
            .build())
        .addMethod(MethodSpec.methodBuilder("newArray")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(ArrayTypeName.of(type))
            .addParameter(int.class, "size")
            .addStatement("return new $T[size]", type)
            .build())
        .build();

    return FieldSpec
        .builder(creatorOfClass, "CREATOR", PUBLIC, FINAL, STATIC)
        .initializer("$L", creatorImpl)
        .build();
  }

  MethodSpec generateWriteToParcel(ProcessingEnvironment env, List<Property> properties,
      Map<TypeMirror, FieldSpec> typeAdapters) {
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
      if (p.typeAdapter != null && typeAdapters.containsKey(p.typeAdapter)) {
        FieldSpec typeAdapter = typeAdapters.get(p.typeAdapter);
        builder.addCode(Parcelables.writeValueWithTypeAdapter(typeAdapter, p, dest));
      } else {
        builder.addCode(Parcelables.writeValue(typeUtils, p, dest));
      }
    }

    return builder.build();
  }

  private static AnnotationSpec createSuppressUncheckedWarningAnnotation() {
    return AnnotationSpec.builder(SuppressWarnings.class)
      .addMember("value", "\"unchecked\"")
      .build();
  }
  private ImmutableMap<TypeMirror, FieldSpec> getTypeAdapters(List<Property> properties) {
    Map<TypeMirror, FieldSpec> typeAdapters = new LinkedHashMap<>();
    NameAllocator nameAllocator = new NameAllocator();
    nameAllocator.newName("CREATOR");
    for (Property property : properties) {
      if (property.typeAdapter != null && !typeAdapters.containsKey(property.typeAdapter)) {
        ClassName typeName = (ClassName) TypeName.get(property.typeAdapter);
        String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, typeName.simpleName());
        name = nameAllocator.newName(name, typeName);

        typeAdapters.put(property.typeAdapter, FieldSpec.builder(
            typeName, NameAllocator.toJavaIdentifier(name), PRIVATE, STATIC, FINAL)
            .initializer("new $T()", typeName).build());
      }
    }
    return ImmutableMap.copyOf(typeAdapters);
  }

  MethodSpec generateDescribeContents() {
    return MethodSpec.methodBuilder("describeContents")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(int.class)
        .addStatement("return 0")
        .build();
  }
}
