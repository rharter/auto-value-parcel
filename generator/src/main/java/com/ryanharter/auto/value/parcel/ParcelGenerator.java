package com.ryanharter.auto.value.parcel;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Types;

/**
 * Created by rharter on 2/23/17.
 */
public interface ParcelGenerator {

  boolean handles(Types types, ExecutableElement element);

  CodeBlock writeValue(Types types, ParameterSpec out, ExecutableElement element);

  CodeBlock readValue(Types types, ParameterSpec in, ExecutableElement element);
}
