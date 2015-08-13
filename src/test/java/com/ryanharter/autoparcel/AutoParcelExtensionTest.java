package com.ryanharter.autoparcel;

import android.os.Parcelable;
import autovalue.shaded.com.google.common.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.testing.compile.CompilationRule;
import com.ryanharter.autoparcel.util.TestMessager;
import com.ryanharter.autoparcel.util.TestProcessingEnvironment;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public class AutoParcelExtensionTest {

  public @Rule CompilationRule rule = new CompilationRule();

  AutoParcelExtension extension = new AutoParcelExtension();

  private Elements elements;
  private Types types;
  private ProcessingEnvironment processingEnvironment;

  @Before public void setup() {
    Messager messager = new TestMessager();
    elements = rule.getElements();
    types = rule.getTypes();
    processingEnvironment = new TestProcessingEnvironment(messager, elements, types);
  }

  @Test public void throwsForNonParcelableProperty() throws Exception {
    TypeElement type = elements.getTypeElement(SampleTypeWithNonSerializable.class.getCanonicalName());
    AutoValueExtension.Context context = createContext(type);

    try {
      extension.generateClass(context, "Test_AnnotatedType", "SampleTypeWithNonSerializable", true);
      fail();
    } catch (AutoParcelException e) {}
  }

  @Test public void acceptsParcelableProperties() throws Exception {
    TypeElement type = elements.getTypeElement(SampleTypeWithParcelable.class.getCanonicalName());
    AutoValueExtension.Context context = createContext(type);

    String generated = extension.generateClass(context, "Test_TypeWithParcelable", "SampleTypeWithParcelable", true);
    assertThat(generated).isNotNull();
  }

  @Test public void generatesConstructorUsingAllParams() throws Exception {
    Map<String, TypeName> properties = new LinkedHashMap<String, TypeName>();
    properties.put("bar", TypeName.get(Double.class));
    properties.put("baz", TypeName.get(Integer.class));
    MethodSpec constructor = extension.generateConstructor(properties);
    assertThat(constructor.toString()).isEqualTo(""
        + "Constructor(java.lang.Double bar, java.lang.Integer baz) {\n"
        + "  super(bar, baz);\n"
        + "}\n");
  }

  @Test public void generatesParcelConstructor() throws Exception {
    Map<String, TypeName> properties = new LinkedHashMap<String, TypeName>();
    properties.put("bar", TypeName.get(Double.class));
    properties.put("baz", TypeName.get(Integer.class));

    FieldSpec classLoader = FieldSpec
        .builder(ClassName.get(ClassLoader.class), "CL", Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
        .initializer("$N.class.getClassLoader()", "Foo")
        .build();

    MethodSpec constructor = extension.generateParcelConstructor(properties, classLoader);
    assertThat(constructor.toString()).isEqualTo(""
        + "private Constructor(android.os.Parcel in) {\n"
        + "  this((java.lang.Double) in.readValue(CL), (java.lang.Integer) in.readValue(CL));\n"
        + "}\n");
  }

  @Test public void generatesCreator() throws Exception {
    String className = "foo.bar.Baz";
    FieldSpec classLoader = FieldSpec
        .builder(ClassName.get(ClassLoader.class), "CL", Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
        .initializer("$N.class.getClassLoader()", className)
        .build();

    FieldSpec creator = extension.generateCreator(className, classLoader);
    assertThat(creator.toString()).isEqualTo(""
        + "private static final android.os.Parcelable.Creator<foo.bar.Baz> CREATOR = new android.os.Parcelable.Creator<foo.bar.Baz>() {\n"
        + "  @java.lang.Override\n"
        + "  public foo.bar.Baz createFromParcel(android.os.Parcel in) {\n"
        + "    return new foo.bar.Baz(in);\n"
        + "  }\n"
        + "\n"
        + "  @java.lang.Override\n"
        + "  public foo.bar.Baz[] newArray(int size) {\n"
        + "    return new foo.bar.Baz[size];\n"
        + "  }\n"
        + "};\n");
  }

  @Test public void generatesWriteToParcel() throws Exception {
    Map<String, TypeName> properties = new LinkedHashMap<String, TypeName>();
    properties.put("bar", TypeName.get(Double.class));
    properties.put("baz", TypeName.get(Integer.class));
    MethodSpec writeToParcel = extension.generateWriteToParcel(properties);
    assertThat(writeToParcel.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public void writeToParcel(android.os.Parcel dest, int flags) {\n"
        + "  dest.writeValue(bar());\n"
        + "  dest.writeValue(baz());\n"
        + "}\n");
  }

  @Test public void generatesDescribeContents() throws Exception {
    MethodSpec describeContents = extension.generateDescribeContents();
    assertThat(describeContents.toString()).isEqualTo(""
        + "@java.lang.Override\n"
        + "public int describeContents() {\n"
        + "  return 0;\n"
        + "}\n");
  }


  private AutoValueExtension.Context createContext(TypeElement type) {
    String packageName = MoreElements.getPackage(type).getQualifiedName().toString();
    Set<ExecutableElement> allMethods = MoreElements.getLocalAndInheritedMethods(type, elements);
    Set<ExecutableElement> methods = methodsToImplement(type, allMethods);
    Map<String, ExecutableElement> properties = new LinkedHashMap<String, ExecutableElement>();
    for (ExecutableElement e : methods) {
      properties.put(e.getSimpleName().toString(), e);
    }

    return new TestContext(processingEnvironment, packageName, type, properties);
  }

  private static class TestContext implements AutoValueExtension.Context {

    private final ProcessingEnvironment processingEnvironment;
    private final String packageName;
    private final TypeElement autoValueClass;
    private final Map<String, ExecutableElement> properties;

    public TestContext(ProcessingEnvironment processingEnvironment, String packageName,
                       TypeElement autoValueClass, Map<String, ExecutableElement> properties) {
      this.processingEnvironment = processingEnvironment;
      this.packageName = packageName;
      this.autoValueClass = autoValueClass;
      this.properties = properties;
    }

    public ProcessingEnvironment processingEnvironment() {
      return processingEnvironment;
    }

    public String packageName() {
      return packageName;
    }

    public TypeElement autoValueClass() {
      return autoValueClass;
    }

    public Map<String, ExecutableElement> properties() {
      return properties;
    }
  }

  abstract class SampleTypeWithNonSerializable implements Parcelable {
    abstract int primitive();
    abstract String serializable();
    abstract NonSerializable nonSerializable();
  }

  abstract class SampleTypeWithParcelable implements Parcelable {
    abstract int primitive();
    abstract String serializable();
    abstract ParcelableProperty parcelable();
  }

  abstract class ParcelableProperty implements Parcelable {}

  class NonSerializable {}

  private ImmutableSet<ExecutableElement> methodsToImplement(
      TypeElement autoValueClass, Set<ExecutableElement> methods) {
    ImmutableSet.Builder<ExecutableElement> toImplement = ImmutableSet.builder();
    for (ExecutableElement method : methods) {
      if (method.getModifiers().contains(Modifier.ABSTRACT)
          && !Arrays.asList("toString", "hashCode", "equals").contains(method.getSimpleName())) {
        if (method.getParameters().isEmpty() && method.getReturnType().getKind() != TypeKind.VOID) {
          toImplement.add(method);
        }
      }
    }
    return toImplement.build();
  }

}