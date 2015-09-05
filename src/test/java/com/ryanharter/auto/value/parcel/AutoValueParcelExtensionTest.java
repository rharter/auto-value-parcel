package com.ryanharter.auto.value.parcel;

import android.os.Parcelable;

import com.google.auto.common.MoreElements;
import com.google.auto.value.AutoValueExtension;
import com.google.auto.value.processor.AutoValueProcessor;
import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.CompilationRule;
import com.google.testing.compile.JavaFileObjects;
import com.ryanharter.auto.value.parcel.util.TestMessager;
import com.ryanharter.auto.value.parcel.util.TestProcessingEnvironment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static org.junit.Assert.fail;

public class AutoValueParcelExtensionTest {

  public @Rule CompilationRule rule = new CompilationRule();

  AutoValueParcelExtension extension = new AutoValueParcelExtension();

  private Elements elements;
  private ProcessingEnvironment processingEnvironment;

  private JavaFileObject parcelable;
  private JavaFileObject parcel;

  @Before public void setup() {
    Messager messager = new TestMessager();
    elements = rule.getElements();
    processingEnvironment = new TestProcessingEnvironment(messager, elements, rule.getTypes());

    parcelable = JavaFileObjects.forSourceString("android.os.Parcelable", ""
        + "package android.os;\n"
        + "public interface Parcelable {\n"
        + "public interface Creator<T> {\n"
        + "  public T createFromParcel(Parcel source);\n"
        + "  public T[] newArray(int size);\n"
        + "}"
        + "int describeContents();\n"
        + "void writeToParcel(Parcel in, int flags);\n"
        + "}\n");
    parcel = JavaFileObjects.forSourceString("android.os.Parcel", ""
        + "package android.os;\n"
        + "public interface Parcel {\n"
        + "Object readValue(ClassLoader cl);\n"
        + "void writeValue(Object o);\n"
        + "}");
  }

  @Test public void simple() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import android.os.Parcelable;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test implements Parcelable {\n"
            + "public abstract int a();\n"
            + "public abstract Double b();\n"
            + "public abstract String c();\n"
            // TODO get rid of this soon!
            + "public int describeContents() {\n"
            + "return 0;\n"
            + "}"
            + "}"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n"
        + "import android.os.Parcel;\n"
        + "import android.os.Parcelable;\n"
        + "import java.lang.ClassLoader;\n"
        + "import java.lang.Double;\n"
        + "import java.lang.Integer;\n"
        + "import java.lang.Override;\n"
        + "import java.lang.String;\n"
        + "final class AutoValue_Test extends $AutoValue_Test {\n"
        + "  private static final ClassLoader CL = AutoValue_Test.class.getClassLoader();\n"
        + "  public static final Parcelable.Creator<AutoValue_Test> CREATOR = new android.os.Parcelable.Creator<AutoValue_Test>() {\n"
        + "    @java.lang.Override\n"
        + "    public AutoValue_Test createFromParcel(android.os.Parcel in) {\n"
        + "      return new AutoValue_Test(in);\n"
        + "    }\n"
        + "    @java.lang.Override\n"
        + "    public AutoValue_Test[] newArray(int size) {\n"
        + "      return new AutoValue_Test[size];\n"
        + "    }\n"
        + "  };\n"
        + "  AutoValue_Test(int a, Double b, String c) {\n"
        + "    super(a, b, c);\n"
        + "  }\n"
        + "  private AutoValue_Test(Parcel in) {\n"
        + "    this((Integer) in.readValue(CL), (Double) in.readValue(CL), (String) in.readValue(CL));\n"
        + "  }\n"
        + "  @Override\n"
        + "  public void writeToParcel(Parcel dest, int flags) {\n"
        + "    dest.writeValue(a());\n"
        + "    dest.writeValue(b());\n"
        + "    dest.writeValue(c());\n"
        + "  }\n"
        + "  @Override\n"
        + "  public int describeContents() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void throwsForNonParcelableProperty() throws Exception {
    TypeElement type = elements.getTypeElement(SampleTypeWithNonSerializable.class.getCanonicalName());
    AutoValueExtension.Context context = createContext(type);

    try {
      extension.generateClass(context, "Test_AnnotatedType", "SampleTypeWithNonSerializable", true);
      fail();
    } catch (AutoValueParcelException e) {}
  }

  @Test public void acceptsParcelableProperties() throws Exception {
    TypeElement type = elements.getTypeElement(SampleTypeWithParcelable.class.getCanonicalName());
    AutoValueExtension.Context context = createContext(type);

    String generated = extension.generateClass(context, "Test_TypeWithParcelable", "SampleTypeWithParcelable", true);
    assertThat(generated).isNotNull();
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