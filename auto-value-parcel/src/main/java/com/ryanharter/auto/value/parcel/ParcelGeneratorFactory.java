package com.ryanharter.auto.value.parcel;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Types;

final class ParcelGeneratorFactory implements ParcelGenerator {

  private static final ImmutableList<? extends ParcelGenerator> builtInGenerators = ImmutableList.of(
      new IntParcelGenerator(),
      new StringParcelGenerator()
  );

  public ParcelGeneratorFactory() {
    // TODO get user supplied generators from ServiceLoader
  }

  private ParcelGenerator getGenerator(Types types, ExecutableElement element) {
    for (ParcelGenerator generator : builtInGenerators) {
      if (generator.handles(types, element)) {
        return generator;
      }
    }
    return null;
  }

  @Override public boolean handles(Types types, ExecutableElement element) {
    if (hasParcelAdapter(element)) {
      return true;
    }

    return getGenerator(types, element) != null;
  }

  private boolean hasParcelAdapter(ExecutableElement element) {
    // TODO Check for ParcelAdapter annotation
    return false;
  }

  @Override
  public CodeBlock writeValue(Types types, ParameterSpec out, ExecutableElement element) {
    ParcelGenerator generator = getGenerator(types, element);
    if (generator == null) {
      throw new IllegalArgumentException("Generator for type " + element + " not found.");
    }

    return generator.writeValue(types, out, element);
  }

  @Override public CodeBlock readValue(Types types, ParameterSpec in, ExecutableElement element) {
    ParcelGenerator generator = getGenerator(types, element);
    if (generator == null) {
      throw new IllegalArgumentException("Generator for type " + element + " not found.");
    }

    return generator.readValue(types, in, element);
  }

  static final class StringParcelGenerator implements ParcelGenerator {

    static final TypeName STRING = ClassName.get("java.lang", "String");

    @Override public boolean handles(Types types, ExecutableElement element) {
      return STRING.equals(TypeName.get(element.getReturnType()));
    }

    @Override
    public CodeBlock writeValue(Types types, ParameterSpec out, ExecutableElement element) {
      return CodeBlock.of("$N.writeString($N())", out, element.getSimpleName());
    }

    @Override
    public CodeBlock readValue(Types types, ParameterSpec in, ExecutableElement element) {
      return CodeBlock.of("$N.readString()", in);
    }
  }

  static final class IntParcelGenerator implements ParcelGenerator {

    @Override public boolean handles(Types types, ExecutableElement element) {
      TypeName type = TypeName.get(element.getReturnType());
      return type.equals(TypeName.BYTE) || type.equals(TypeName.BYTE.box())
          || type.equals(TypeName.INT) || type.equals(TypeName.INT.box())
          || type.equals(TypeName.CHAR) || type.equals(TypeName.CHAR.box())
          || type.equals(TypeName.SHORT);
    }

    @Override
    public CodeBlock writeValue(Types types, ParameterSpec out, ExecutableElement element) {
      return CodeBlock.of("$N.writeInt($N())", out, element.getSimpleName());
    }

    @Override public CodeBlock readValue(Types types, ParameterSpec in, ExecutableElement element) {
      CodeBlock.Builder builder = CodeBlock.builder();
      TypeName type = TypeName.get(element.getReturnType());
      if (!type.equals(TypeName.INT) && !type.equals(TypeName.INT.box())) {
        builder.add("($T) ", type);
      }
      return builder.add("$N.readInt()", in).build();
    }
  }
}
