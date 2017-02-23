package com.ryanharter.auto.value.parcel;

import com.google.testing.compile.CompilationRule;
import com.ryanharter.auto.value.parcel.ParcelGeneratorFactory.IntParcelGenerator;
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

public class IntParcelGeneratorTest {

  private ParcelGeneratorTestHelper genHelper;
  @Rule public CompilationRule rule = new CompilationRule();

  private IntParcelGenerator generator;

  @Before public void setup() {
    genHelper = new ParcelGeneratorTestHelper(rule);
    generator = new IntParcelGenerator();
  }

  @Test public void handlesCorrectTypes() {
    ExecutableElement returnsInt = genHelper.getExecutableElement(TestClass.class, "returnsInt");
    assertTrue(generator.handles(genHelper.getTypes(), returnsInt));

    ExecutableElement returnsString = genHelper.getExecutableElement(TestClass.class, "returnsString");
    assertFalse(generator.handles(genHelper.getTypes(), returnsString));

    ExecutableElement returnsVoid = genHelper.getExecutableElement(TestClass.class, "returnsVoid");
    assertFalse(generator.handles(genHelper.getTypes(), returnsVoid));
  }

  @Test public void createsWriteMethod() {
    ParameterSpec out = ParameterSpec.builder(ClassName.get("android.os", "Parcel"), "out").build();

    ExecutableElement element = genHelper.getExecutableElement(TestClass.class, "returnsInt");
    CodeBlock expected = CodeBlock.of("out.writeInt(returnsInt())");
    CodeBlock actual = generator.writeValue(genHelper.getTypes(), out, element);
    assertThat(actual).isEqualTo(expected);

    element = genHelper.getExecutableElement(TestClass.class, "returnsShort");
    expected = CodeBlock.of("out.writeInt(returnsShort())");
    actual = generator.writeValue(genHelper.getTypes(), out, element);
    assertThat(actual).isEqualTo(expected);
  }

  @Test public void createsReadMethod() {
    ParameterSpec in = ParameterSpec.builder(ClassName.get("android.os", "Parcel"), "in").build();

    ExecutableElement element = genHelper.getExecutableElement(TestClass.class, "returnsInt");
    CodeBlock expected = CodeBlock.of("in.readInt()");
    CodeBlock actual = generator.readValue(genHelper.getTypes(), in, element);
    assertThat(actual).isEqualTo(expected);

    element = genHelper.getExecutableElement(TestClass.class, "returnsShort");
    expected = CodeBlock.of("(short) in.readInt()");
    actual = generator.readValue(genHelper.getTypes(), in, element);
    assertThat(actual).isEqualTo(expected);
  }

  private static class TestClass {
    public String returnsString() { return ""; }
    public void returnsVoid() {}
    public int returnsInt() { return 0; }
    public short returnsShort() { return 0; }
    public char returnsChar() { return 'a'; }
  }
}