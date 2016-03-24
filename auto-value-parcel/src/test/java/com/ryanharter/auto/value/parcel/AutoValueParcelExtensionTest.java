package com.ryanharter.auto.value.parcel;

import android.os.Parcelable;

import com.google.auto.common.MoreElements;
import com.google.auto.value.extension.AutoValueExtension;
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

  @Rule public CompilationRule rule = new CompilationRule();

  AutoValueParcelExtension extension = new AutoValueParcelExtension();

  private Elements elements;
  private ProcessingEnvironment processingEnvironment;

  private JavaFileObject parcelable;
  private JavaFileObject parcel;
  private JavaFileObject nullable;

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
    parcel = JavaFileObjects.forSourceString("android.os.Parcel", "" +
        "package android.os;\n" +
        "import java.util.HashMap;\n" +
        "import java.util.Map;\n" +
        "import java.util.List;\n" +
        "import java.util.ArrayList;\n" +
        "import java.io.Serializable;\n" +
        "import android.util.SparseArray;\n" +
        "import android.util.SparseBooleanArray;\n" +
        "import android.util.Size;\n" +
        "import android.util.SizeF;\n" +
        "public interface Parcel {\n" +
        "Object readValue(ClassLoader cl);\n" +
        "void writeValue(Object o);\n" +
        "  byte readByte();\n" +
        "  int readInt();\n" +
        "  long readLong();\n" +
        "  float readFloat();\n" +
        "  double readDouble();\n" +
        "  String readString();\n" +
        "  Parcelable readParcelable(ClassLoader cl);\n" +
        "  CharSequence readCharSequence();\n" +
        "  HashMap readHashMap(ClassLoader cl);\n" +
        "  ArrayList readArrayList(ClassLoader cl);\n" +
        "  boolean[] createBooleanArray();\n" +
        "  byte[] createByteArray();\n" +
        "  int[] createIntArray();\n" +
        "  long[] createLongArray();\n" +
        "  String[] readStringArray();\n" +
        "  Serializable readSerializable();\n" +
        "  SparseArray readSparseArray(ClassLoader cl);\n" +
        "  SparseBooleanArray readSparseBooleanArray();\n" +
        "  Bundle readBundle(ClassLoader cl);\n" +
        "  PersistableBundle readPersistableBundle(ClassLoader cl);\n" +
        "  Size readSize();\n" +
        "  SizeF readSizeF();\n" +
        "  IBinder readStrongBinder();\n" +
        "  void writeString(String in);\n" +
        "  void writeParcelable(Parcelable in, int flags);\n" +
        "  void writeCharSequence(CharSequence in);\n" +
        "  void writeMap(Map in);\n" +
        "  void writeList(List in);\n" +
        "  void writeBooleanArray(boolean[] in);\n" +
        "  void writeByteArray(byte[] in);\n" +
        "  void writeIntArray(int[] in);\n" +
        "  void writeLongArray(long[] in);\n" +
        "  void writeSerializable(Serializable in);\n" +
        "  void writeSparseArray(SparseArray in);\n" +
        "  void writeSparseBooleanArray(SparseBooleanArray in);\n" +
        "  void writeBundle(Bundle in);\n" +
        "  void writePersistableBundle(PersistableBundle in);\n" +
        "  void writeSize(Size in);\n" +
        "  void writeSizeF(SizeF in);\n" +
        "  void writeInt(int in);\n" +
        "  void writeLong(long in);\n" +
        "  void writeFloat(float in);\n" +
        "  void writeDouble(double in);\n" +
        "  void writeStrongBinder(IBinder in);\n"
        + "}");
    nullable = JavaFileObjects.forSourceString("com.ryanharter.auto.value.moshi.Nullable", ""
        + "package test;\n"
        + "import java.lang.annotation.Retention;\n"
        + "import java.lang.annotation.Target;\n"
        + "import static java.lang.annotation.ElementType.METHOD;\n"
        + "import static java.lang.annotation.ElementType.PARAMETER;\n"
        + "import static java.lang.annotation.ElementType.FIELD;\n"
        + "import static java.lang.annotation.RetentionPolicy.SOURCE;\n"
        + "@Retention(SOURCE)\n"
        + "@Target({METHOD, PARAMETER, FIELD})\n"
        + "public @interface Nullable {\n"
        + "}");
  }

  @Test public void simple() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test implements Parcelable {\n"
        + "public abstract int a();\n"
        + "@Nullable public abstract Double b();\n"
        + "public abstract String c();\n"
        + "public abstract long d();\n"
        + "}"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n" +
        "\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import java.lang.Double;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.String;\n" +
        "\n" +
        "final class AutoValue_Test extends $AutoValue_Test {\n" +
        "  public static final Parcelable.Creator<AutoValue_Test> CREATOR = new Parcelable.Creator<AutoValue_Test>() {\n" +
        "\n" +
        "    @Override\n" +
        "    public AutoValue_Test createFromParcel(Parcel in) {\n" +
        "      return new AutoValue_Test(\n" +
        "          in.readInt(),\n" +
        "          in.readInt() == 0 ? (Double) in.readSerializable() : null,\n" +
        "          in.readString(),\n" +
        "          in.readLong()\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_Test[] newArray(int size) {\n" +
        "      return new AutoValue_Test[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_Test(int a, Double b, String c, long d) {\n" +
        "    super(a, b, c, d);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    dest.writeInt(a());\n" +
        "    if (b() == null) {\n" +
        "      dest.writeInt(1);\n" +
        "    } else {\n" +
        "      dest.writeInt(0);\n" +
        "      dest.writeSerializable(b());\n" +
        "    }\n" +
        "    dest.writeString(c());\n" +
        "    dest.writeLong(d());\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public int describeContents() {\n" +
        "    return 0;\n" +
        "  }\n" +
        "}");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, nullable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void propertyMethodReferencedWithPrefix() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.AbstractParcelable", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "public abstract class AbstractParcelable implements Parcelable {\n"
        + "  @Override public final int describeContents() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}"
    );
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test extends AbstractParcelable {\n"
        + "  public abstract String getName();\n"
        + "  public abstract boolean isAwesome();\n"
        + "}"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n" +
        "\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.String;\n" +
        "\n" +
        "final class AutoValue_Test extends $AutoValue_Test {\n" +
        "  public static final Parcelable.Creator<AutoValue_Test> CREATOR = new Parcelable.Creator<AutoValue_Test>() {\n" +
        "\n" +
        "    @Override\n" +
        "    public AutoValue_Test createFromParcel(Parcel in) {\n" +
        "      return new AutoValue_Test(\n" +
        "          in.readString(),\n" +
        "          in.readInt() == 1\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_Test[] newArray(int size) {\n" +
        "      return new AutoValue_Test[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_Test(String name, boolean awesome) {\n" +
        "    super(name, awesome);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    dest.writeString(getName());\n" +
        "    dest.writeInt(isAwesome() ? 1 : 0);\n" +
        "  }\n" +
        "}");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, nullable, source1, source2))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void builder() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Test implements Parcelable {\n"
        + "public abstract int a();\n"
        + "public abstract Double b();\n"
        + "public abstract String c();\n"
        + "@AutoValue.Builder public static abstract class Builder {\n"
        + "  public abstract Builder a(int a);\n"
        + "  public abstract Builder b(Double b);\n"
        + "  public abstract Builder c(String c);\n"
        + "  public abstract Test build();\n"
        + "}\n"
        + "}"
    );

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test public void handlesParcelableCollectionTypes() {
    JavaFileObject parcelable1 = JavaFileObjects.forSourceString("test.Parcelable1", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import android.os.Parcel;\n"
        + "public class Parcelable1 implements Parcelable {\n"
        + "  public int describeContents() {"
        + "    return 0;"
        + "  }\n"
        + "  public void writeToParcel(Parcel in, int flags) {"
        + "  }\n"
        + "}");
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import java.util.List;\n"
        + "@AutoValue public abstract class Test implements Parcelable {\n"
          + "public abstract List<Parcelable1> a();\n"
          + "public abstract int[] b();\n"
        + "}"
    );

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
        + "package test;\n" +
        "\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import java.lang.ClassLoader;\n" +
        "import java.lang.Override;\n" +
        "import java.util.List;\n" +
        "\n" +
        "final class AutoValue_Test extends $AutoValue_Test {\n" +
        "  public static final Parcelable.Creator<AutoValue_Test> CREATOR = new Parcelable.Creator<AutoValue_Test>() {\n" +
        "    @Override\n" +
        "    public AutoValue_Test createFromParcel(Parcel in) {\n" +
        "      ClassLoader cl = AutoValue_Test.class.getClassLoader();\n" +
        "      return new AutoValue_Test(\n" +
        "        (List<Parcelable1>) in.readArrayList(cl),\n" +
        "        in.createIntArray()\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_Test[] newArray(int size) {\n" +
        "      return new AutoValue_Test[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_Test(List<Parcelable1> a, int[] b) {\n" +
        "    super(a, b);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    dest.writeList(a());\n" +
        "    dest.writeIntArray(b());\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public int describeContents() {\n" +
        "    return 0;\n" +
        "  }\n" +
        "}");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, parcelable1, source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void describeContentsOmittedWhenAlreadyDefined() {
    JavaFileObject notMatching = JavaFileObjects.forSourceString("test.No", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class No implements Parcelable {\n"
        + "  public abstract String name();\n"
        + "  @Override public abstract int describeContents();\n"
        + "  public int describeContents(String name) {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}");
    JavaFileObject matching = JavaFileObjects.forSourceString("test.Yes", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Yes implements Parcelable {\n"
        + "  public abstract String name();\n"
        + "public int describeContents() {\n"
        + "  return 0;\n"
        + "}\n"
        + "}"
    );

    JavaFileObject expectedNotMatching = JavaFileObjects.forSourceString("test/AutoValue_No", ""
        + "package test;\n" +
        "\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.String;\n" +
        "\n" +
        "final class AutoValue_No extends $AutoValue_No {\n" +
        "  public static final Parcelable.Creator<AutoValue_No> CREATOR = new Parcelable.Creator<AutoValue_No>() {\n" +
        "    @Override\n" +
        "    public AutoValue_No createFromParcel(Parcel in) {\n" +
        "      return new AutoValue_No(\n" +
        "        in.readString()\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_No[] newArray(int size) {\n" +
        "      return new AutoValue_No[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_No(String name) {\n" +
        "    super(name);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    dest.writeString(name());\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public int describeContents() {\n" +
        "    return 0;\n" +
        "  }\n" +
        "}");

    JavaFileObject expectedMatching = JavaFileObjects.forSourceString("test/AutoValue_Yes", ""
        + "package test;\n" +
        "\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.String;\n" +
        "\n" +
        "final class AutoValue_Yes extends $AutoValue_Yes {\n" +
        "  public static final Parcelable.Creator<AutoValue_Yes> CREATOR = new Parcelable.Creator<AutoValue_Yes>() {\n" +
        "    @Override\n" +
        "    public AutoValue_Yes createFromParcel(Parcel in) {\n" +
        "      return new AutoValue_Yes(\n" +
        "        in.readString()\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_Yes[] newArray(int size) {\n" +
        "      return new AutoValue_Yes[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_Yes(String name) {\n" +
        "    super(name);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    dest.writeString(name());\n" +
        "  }\n" +
        "}");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, notMatching, matching))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedNotMatching, expectedMatching);
  }

  @Test public void handlesAllParcelableTypes() {
    JavaFileObject parcelable1 = JavaFileObjects.forSourceString("test.Parcelable1", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import android.os.Parcel;\n"
        + "public class Parcelable1 implements Parcelable {\n"
        + "  public int describeContents() {"
        + "    return 0;"
        + "  }\n"
        + "  public void writeToParcel(Parcel in, int flags) {"
        + "  }\n"
        + "}");
    JavaFileObject foobinder = JavaFileObjects.forSourceString("test.FooBinder", "" +
        "package test;\n" +
        "import android.os.IBinder;\n" +
        "public class FooBinder implements IBinder {\n" +
        "}\n");
    JavaFileObject source = JavaFileObjects.forSourceString("test.Foo", "" +
        "package test;\n" +
        "\n" +
        "import android.os.Bundle;\n" +
        "import android.os.IBinder;\n" +
        "import android.os.Parcelable;\n" +
        "import android.os.PersistableBundle;\n" +
        "import android.util.SizeF;\n" +
        "import android.util.Size;\n" +
        "import android.util.SparseArray;\n" +
        "import android.util.SparseBooleanArray;\n" +
        "import com.google.auto.value.AutoValue;\n" +
        "import java.io.Serializable;\n" +
        "import java.util.List;\n" +
        "import java.util.Map;\n" +
        "\n" +
        "@AutoValue public abstract class Foo implements Parcelable {\n" +
        "  @Nullable public abstract String a();\n" +
        "  public abstract byte b();\n" +
        "  public abstract int c();\n" +
        "  public abstract short d();\n" +
        "  public abstract long e();\n" +
        "  public abstract float f();\n" +
        "  public abstract double g();\n" +
        "  public abstract boolean h();\n" +
        "  public abstract Parcelable i();\n" +
        "  public abstract CharSequence j();\n" +
        "  public abstract Map<String, String> k();\n" +
        "  public abstract List<String> l();\n" +
        "  public abstract boolean[] m();\n" +
        "  public abstract byte[] n();\n" +
        "  public abstract int[] s();\n" +
        "  public abstract long[] t();\n" +
        "  public abstract Serializable u();\n" +
        "  public abstract SparseArray w();\n" +
        "  public abstract SparseBooleanArray x();\n" +
        "  public abstract Bundle y();\n" +
        "  public abstract PersistableBundle z();\n" +
        "  public abstract Size aa();\n" +
        "  public abstract SizeF ab();\n" +
        "  @Nullable public abstract Parcelable1 ad();\n" +
        "  public abstract FooBinder ae();\n" +
        "  @Nullable public abstract Boolean af();\n" +
        "}");

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Foo", "" +
        "package test;\n" +
        "\n" +
        "import android.os.Bundle;\n" +
        "import android.os.Parcel;\n" +
        "import android.os.Parcelable;\n" +
        "import android.os.PersistableBundle;\n" +
        "import android.util.Size;\n" +
        "import android.util.SizeF;\n" +
        "import android.util.SparseArray;\n" +
        "import android.util.SparseBooleanArray;\n" +
        "import java.io.Serializable;\n" +
        "import java.lang.Boolean;\n" +
        "import java.lang.CharSequence;\n" +
        "import java.lang.ClassLoader;\n" +
        "import java.lang.Override;\n" +
        "import java.lang.String;\n" +
        "import java.util.List;\n" +
        "import java.util.Map;\n" +
        "\n" +
        "final class AutoValue_Foo extends $AutoValue_Foo {\n" +
        "  public static final Parcelable.Creator<AutoValue_Foo> CREATOR = new Parcelable.Creator<AutoValue_Foo>() {\n" +
        "    @Override\n" +
        "    public AutoValue_Foo createFromParcel(Parcel in) {\n" +
        "      ClassLoader cl = AutoValue_Foo.class.getClassLoader();\n" +
        "      return new AutoValue_Foo(\n" +
        "        in.readInt() == 0 ? in.readString() : null,\n" +
        "        in.readByte(),\n" +
        "        in.readInt(),\n" +
        "        (short) in.readInt(),\n" +
        "        in.readLong(),\n" +
        "        in.readFloat(),\n" +
        "        in.readDouble(),\n" +
        "        in.readInt() == 1,\n" +
        "        (Parcelable) in.readParcelable(cl),\n" +
        "        (CharSequence) in.readCharSequence(),\n" +
        "        (Map<String, String>) in.readHashMap(cl),\n" +
        "        (List<String>) in.readArrayList(cl),\n" +
        "        in.createBooleanArray(),\n" +
        "        in.createByteArray(),\n" +
        "        in.createIntArray(),\n" +
        "        in.createLongArray(),\n" +
        "        (Serializable) in.readSerializable(),\n" +
        "        in.readSparseArray(cl),\n" +
        "        in.readSparseBooleanArray(),\n" +
        "        in.readBundle(cl),\n" +
        "        in.readPersistableBundle(cl),\n" +
        "        in.readSize(),\n" +
        "        in.readSizeF(),\n" +
        "        in.readInt() == 0 ? (Parcelable1) in.readParcelable(cl) : null,\n" +
        "        (FooBinder) in.readStrongBinder(),\n" +
        "        in.readInt() == 0 ? (Boolean) in.readSerializable() : null\n" +
        "      );\n" +
        "    }\n" +
        "    @Override\n" +
        "    public AutoValue_Foo[] newArray(int size) {\n" +
        "      return new AutoValue_Foo[size];\n" +
        "    }\n" +
        "  };\n" +
        "\n" +
        "  AutoValue_Foo(String a, byte b, int c, short d, long e, float f, double g, boolean h, Parcelable i, CharSequence j, Map<String, String> k, List<String> l, boolean[] m, byte[] n, int[] s, long[] t, Serializable u, SparseArray w, SparseBooleanArray x, Bundle y, PersistableBundle z, Size aa, SizeF ab, Parcelable1 ad, FooBinder ae, Boolean af) {\n" +
        "    super(a, b, c, d, e, f, g, h, i, j, k, l, m, n, s, t, u, w, x, y, z, aa, ab, ad, ae, af);\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public void writeToParcel(Parcel dest, int flags) {\n" +
        "    if (a() == null) {\n" +
        "      dest.writeInt(1);\n" +
        "    } else {\n" +
        "      dest.writeInt(0);\n" +
        "      dest.writeString(a());\n" +
        "    }\n" +
        "    dest.writeInt(b());\n" +
        "    dest.writeInt(c());\n" +
        "    dest.writeInt(((Short) d()).intValue());\n" +
        "    dest.writeLong(e());\n" +
        "    dest.writeFloat(f());\n" +
        "    dest.writeDouble(g());\n" +
        "    dest.writeInt(h() ? 1 : 0);\n" +
        "    dest.writeParcelable(i(), 0);\n" +
        "    dest.writeCharSequence(j());\n" +
        "    dest.writeMap(k());\n" +
        "    dest.writeList(l());\n" +
        "    dest.writeBooleanArray(m());\n" +
        "    dest.writeByteArray(n());\n" +
        "    dest.writeIntArray(s());\n" +
        "    dest.writeLongArray(t());\n" +
        "    dest.writeSerializable(u());\n" +
        "    dest.writeSparseArray(w());\n" +
        "    dest.writeSparseBooleanArray(x());\n" +
        "    dest.writeBundle(y());\n" +
        "    dest.writePersistableBundle(z());\n" +
        "    dest.writeSize(aa());\n" +
        "    dest.writeSizeF(ab());\n" +
        "    if (ad() == null) {\n" +
        "      dest.writeInt(1);\n" +
        "    } else {\n" +
        "      dest.writeInt(0);\n" +
        "      dest.writeParcelable(ad(), 0);\n" +
        "    }\n" +
        "    dest.writeStrongBinder(ae());\n" +
        "    if (af() == null) {\n" +
        "      dest.writeInt(1);\n" +
        "    } else {\n" +
        "      dest.writeInt(0);\n" +
        "      dest.writeSerializable(af());\n" +
        "    }\n" +
        "  }\n" +
        "\n" +
        "  @Override\n" +
        "  public int describeContents() {\n" +
        "    return 0;\n" +
        "  }\n" +
        "}");

    assertAbout(javaSources())
        .that(Arrays.asList(nullable, parcel, parcelable, parcelable1, foobinder, source))
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

  @Test public void usesCustomParcelTypeAdapter() throws Exception {
    JavaFileObject bar = JavaFileObjects.forSourceString("test.Bar", ""
        + "package test;\n"
        + "import java.util.Date;\n"
        + "public class Bar {\n"
        + "  public Date date;\n"
        + "  public boolean valid;\n"
        + "  public Bar(Date date, boolean valid) {\n"
        + "    this.date = date;\n"
        + "    this.valid = valid;\n"
        + "  }\n"
        + "}");
    JavaFileObject barAdapter = JavaFileObjects.forSourceString("test.BarTypeAdapter", ""
        + "package test;\n"
        + "import android.os.Parcel;\n"
        + "import java.util.Date;\n"
        + "import com.ryanharter.auto.value.parcel.TypeAdapter;\n"
        + "public class BarTypeAdapter implements TypeAdapter<Bar> {\n"
        + "\n"
        + "  public Bar fromParcel(Parcel in) {\n"
        + "    return new Bar(\n"
        + "        new Date(in.readLong()),\n"
        + "        in.readInt() == 1);\n"
        + "  }\n"
        + "\n"
        + "  public void toParcel(Bar value, Parcel dest) {\n"
        + "    dest.writeLong(value.date.getTime());\n"
        + "    dest.writeInt(value.valid ? 1 : 0);\n"
        + "  }\n"
        + "}\n");
    JavaFileObject foo = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "import com.ryanharter.auto.value.parcel.ParcelAdapter;\n"
        + "@AutoValue public abstract class Foo implements Parcelable {\n"
        + "  @ParcelAdapter(BarTypeAdapter.class) public abstract Bar bar();\n"
        + "}\n");

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Foo", ""
        + "package test;\n"
        + "\n"
        + "import android.os.Parcel;\n"
        + "import android.os.Parcelable;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "final class AutoValue_Foo extends $AutoValue_Foo {\n"
        + "  public static final Parcelable.Creator<AutoValue_Foo> CREATOR = new Parcelable.Creator<AutoValue_Foo>() {\n"
        + "    @Override\n"
        + "    public AutoValue_Foo createFromParcel(Parcel in) {\n"
        + "      BarTypeAdapter barTypeAdapter = new BarTypeAdapter();\n"
        + "      return new AutoValue_Foo(\n"
        + "        barTypeAdapter.fromParcel(in)\n"
        + "      );\n"
        + "    }\n"
        + "    @Override\n"
        + "    public AutoValue_Foo[] newArray(int size) {\n"
        + "      return new AutoValue_Foo[size];\n"
        + "    }\n"
        + "  };\n"
        + "\n"
        + "  AutoValue_Foo(Bar bar) {\n"
        + "    super(bar);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public void writeToParcel(Parcel dest, int flags) {\n"
        + "    BarTypeAdapter barTypeAdapter = new BarTypeAdapter();\n"
        + "    barTypeAdapter.toParcel(bar(), dest);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public int describeContents() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}\n");

    assertAbout(javaSources())
        .that(Arrays.asList(parcel, parcelable, bar, barAdapter, foo))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
  }

  @Test public void acceptsParcelableSubclassesTwiceRemoved() throws Exception {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.SafeParcelable", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "public interface SafeParcelable extends Parcelable {\n"
        + "}");
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.Param", ""
        + "package test;\n"
        + "import android.os.Parcel;\n"
        + "public class Param implements SafeParcelable {\n"
        + "  public int describeContents() { return 0; }\n"
        + "  public void writeToParcel(Parcel p, int i) {}\n"
        + "  public static final Creator<Param> CREATOR = null;\n"
        + "}\n");
    JavaFileObject source3 = JavaFileObjects.forSourceString("test.Foo", ""
        + "package test;\n"
        + "import android.os.Parcelable;\n"
        + "import com.google.auto.value.AutoValue;\n"
        + "@AutoValue public abstract class Foo implements Parcelable {\n"
        + "  public abstract Param param();\n"
        + "}\n");

    JavaFileObject expected = JavaFileObjects.forSourceString("test/AutoValue_Foo", ""
        + "package test;\n"
        + "\n"
        + "import android.os.Parcel;\n"
        + "import android.os.Parcelable;\n"
        + "import java.lang.ClassLoader;\n"
        + "import java.lang.Override;\n"
        + "\n"
        + "final class AutoValue_Foo extends $AutoValue_Foo {\n"
        + "  public static final Parcelable.Creator<AutoValue_Foo> CREATOR = new Parcelable.Creator<AutoValue_Foo>() {\n"
        + "    @Override\n"
        + "    public AutoValue_Foo createFromParcel(Parcel in) {\n"
        + "      ClassLoader cl = AutoValue_Foo.class.getClassLoader();\n"
        + "      return new AutoValue_Foo(\n"
        + "          (Param) in.readParcelable(cl)\n"
        + "      );\n"
        + "    }\n"
        + "    @Override\n"
        + "    public AutoValue_Foo[] newArray(int size) {\n"
        + "      return new AutoValue_Foo[size];\n"
        + "    }\n"
        + "  };\n"
        + "\n"
        + "  AutoValue_Foo(Param param) {\n"
        + "    super(param);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public void writeToParcel(Parcel dest, int flags) {\n"
        + "    dest.writeParcelable(param(), 0);\n"
        + "  }\n"
        + "\n"
        + "  @Override\n"
        + "  public int describeContents() {\n"
        + "    return 0;\n"
        + "  }\n"
        + "}\n");

    assertAbout(javaSources())
        .that(Arrays.asList(parcelable, parcel, source1, source2, source3))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expected);
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
          && !Arrays.asList("toString", "hashCode", "equals").contains(method.getSimpleName().toString())) {
        if (method.getParameters().isEmpty() && method.getReturnType().getKind() != TypeKind.VOID) {
          toImplement.add(method);
        }
      }
    }
    return toImplement.build();
  }

}
