package com.ryanharter.autoparcel;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.element.Modifier;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class AutoParcelExtensionTest {

  AutoParcelExtension extension = new AutoParcelExtension();

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
        .initializer("$S.class.getClassLoader()", "Foo")
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
        .initializer("$S.class.getClassLoader()", className)
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
        + "  dest.writeValue(bar);\n"
        + "  dest.writeValue(baz);\n"
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

}