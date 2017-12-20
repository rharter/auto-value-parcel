package com.ryanharter.auto.value.parcel;

import com.google.testing.compile.CompilationRule;
import com.ryanharter.auto.value.parcel.ParcelGeneratorFactory.StringParcelGenerator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import javax.lang.model.element.ExecutableElement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StringParcelGeneratorTest {

  private ParcelGeneratorTestHelper genHelper;
  @Rule public CompilationRule rule = new CompilationRule();

  private StringParcelGenerator generator;

  @Before public void setup() {
    genHelper = new ParcelGeneratorTestHelper(rule);
    generator = new StringParcelGenerator();
  }

  @Test public void handlesCorrectTypes() {
    ExecutableElement returnsString = genHelper.getExecutableElement(TestClass.class, "returnsString");
    assertTrue(generator.handles(genHelper.getTypes(), returnsString));

    ExecutableElement returnsInt = genHelper.getExecutableElement(TestClass.class, "returnsInt");
    assertFalse(generator.handles(genHelper.getTypes(), returnsInt));

    ExecutableElement returnsVoid = genHelper.getExecutableElement(TestClass.class, "returnsVoid");
    assertFalse(generator.handles(genHelper.getTypes(), returnsVoid));
  }

  @Test public void createsWriteMethod() {
    ExecutableElement element = genHelper.getExecutableElement(TestClass.class, "returnsString");
    ParameterSpec out = ParameterSpec.builder(ClassName.get("android.os", "Parcel"), "out").build();
    CodeBlock expected = CodeBlock.of("out.writeString(returnsString())");
    CodeBlock actual = generator.writeValue(genHelper.getTypes(), out, element);
    assertThat(actual).isEqualTo(expected);
  }

  @Test public void createsReadMethod() {
    ExecutableElement element = genHelper.getExecutableElement(TestClass.class, "returnsString");
    ParameterSpec in = ParameterSpec.builder(ClassName.get("android.os", "Parcel"), "in").build();
    CodeBlock expected = CodeBlock.of("in.readString()");
    CodeBlock actual = generator.readValue(genHelper.getTypes(), in, element);
    assertThat(actual).isEqualTo(expected);
  }

  private static class TestClass {
    public String returnsString() { return ""; }
    public int returnsInt() { return 0; }
    public void returnsVoid() {}
  }
}