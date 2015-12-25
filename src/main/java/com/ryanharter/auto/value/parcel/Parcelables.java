package com.ryanharter.auto.value.parcel;

import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Set;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Created by rharter on 10/20/15.
 */
final class Parcelables {

  private static final TypeName STRING = ClassName.get("java.lang", "String");
  private static final TypeName MAP = ClassName.get("java.util", "Map");
  private static final TypeName LIST = ClassName.get("java.util", "List");
  private static final TypeName BOOLEANARRAY = ArrayTypeName.of(boolean.class);
  private static final TypeName BYTEARRAY = ArrayTypeName.of(byte.class);
  private static final TypeName INTARRAY = ArrayTypeName.of(int.class);
  private static final TypeName LONGARRAY = ArrayTypeName.of(long.class);
  private static final TypeName STRINGARRAY = ArrayTypeName.of(String.class);
  private static final TypeName SPARSEARRAY = ClassName.get("android.util", "SparseArray");
  private static final TypeName SPARSEBOOLEANARRAY = ClassName.get("android.util", "SparseBooleanArray");
  private static final TypeName BUNDLE = ClassName.get("android.os", "Bundle");
  private static final TypeName PARCELABLE = ClassName.get("android.os", "Parcelable");
  private static final TypeName PARCELABLEARRAY = ArrayTypeName.of(PARCELABLE);
  private static final TypeName CHARSEQUENCE = ClassName.get("java.lang", "CharSequence");
  private static final TypeName CHARSEQUENCEARRAY = ArrayTypeName.of(CHARSEQUENCE);
  private static final TypeName IBINDER = ClassName.get("android.os", "IBinder");
  private static final TypeName OBJECTARRAY = ArrayTypeName.of(TypeName.OBJECT);
  private static final TypeName SERIALIZABLE = ClassName.get("java.io", "Serializable");
  private static final TypeName PERSISTABLEBUNDLE = ClassName.get("android.os", "PersistableBundle");
  private static final TypeName SIZE = ClassName.get("android.util", "Size");
  private static final TypeName SIZEF = ClassName.get("android.util", "SizeF");

  private static final Set<TypeName> VALID_TYPES = ImmutableSet.of(STRING, MAP, LIST, BOOLEANARRAY,
      BYTEARRAY, INTARRAY, LONGARRAY, STRINGARRAY, SPARSEARRAY, SPARSEBOOLEANARRAY, BUNDLE,
      PARCELABLE, PARCELABLEARRAY, CHARSEQUENCE, CHARSEQUENCEARRAY, IBINDER, OBJECTARRAY,
      SERIALIZABLE, PERSISTABLEBUNDLE, SIZE, SIZEF);

  public static boolean isValidType(TypeName typeName) {
    return typeName.isPrimitive() || VALID_TYPES.contains(typeName);
  }

  public static boolean isValidType(Types types, TypeElement type) {
    return getParcelableType(types, type) != null;
  }

  public static TypeName getParcelableType(Types types, TypeElement type) {
    TypeMirror typeMirror = type.asType();
    while (typeMirror.getKind() != TypeKind.NONE) {

      // first, check if the class is valid.
      TypeName typeName = TypeName.get(typeMirror);
      if (typeName instanceof ParameterizedTypeName) {
        typeName = ((ParameterizedTypeName) typeName).rawType;
      }
      if (typeName.isPrimitive() || VALID_TYPES.contains(typeName)) {
        return typeName;
      }

      // then check if it implements valid interfaces
      for (TypeMirror iface : type.getInterfaces()) {
        TypeName ifaceName = TypeName.get(iface);
        if (VALID_TYPES.contains(ifaceName)) {
          return ifaceName;
        }
      }

      // then move on
      type = (TypeElement) types.asElement(typeMirror);
      typeMirror = type.getSuperclass();
    }
    return null;
  }

  public static CodeBlock readValue(Types types, AutoValueParcelExtension.Property property,
      FieldSpec classloader) {
    CodeBlock.Builder block = CodeBlock.builder();

    if (property.nullable()){
      block.add("in.readInt() == 0 ? ");
    }

    TypeElement element = (TypeElement) types.asElement(property.element.getReturnType());
    final TypeName type = element != null ? getParcelableType(types, element) : property.type;
    if (type.equals(STRING))
      block.add("in.readString()");
    else if (type.equals(TypeName.BYTE))
      block.add("in.readByte()");
    else if (type.equals(TypeName.INT))
      block.add("in.readInt()");
    else if (type.equals(TypeName.SHORT))
      block.add("(short) in.readInt()");
    else if (type.equals(TypeName.LONG))
      block.add("in.readLong()");
    else if (type.equals(TypeName.FLOAT))
      block.add("in.readFloat()");
    else if (type.equals(TypeName.DOUBLE))
      block.add("in.readDouble()");
    else if (type.equals(TypeName.BOOLEAN))
      block.add("in.readInt() == 1");
    else if (type.equals(PARCELABLE))
      block.add("($T) in.readParcelable($N)", property.type, classloader);
    else if (type.equals(CHARSEQUENCE))
      block.add("($T) in.readCharSequence()", property.type);
    else if (type.equals(MAP))
      block.add("($T) in.readHashMap($N)", property.type, classloader);
    else if (type.equals(LIST))
      block.add("($T) in.readArrayList($N)", property.type, classloader);
    else if (type.equals(BOOLEANARRAY))
      block.add("in.createBooleanArray()");
    else if (type.equals(BYTEARRAY))
      block.add("in.createByteArray()");
    else if (type.equals(STRINGARRAY))
      block.add("in.readStringArray()");
    else if (type.equals(CHARSEQUENCEARRAY))
      block.add("in.readCharSequenceArray()");
    else if (type.equals(IBINDER))
      block.add("($T) in.readStrongBinder()", property.type);
    else if (type.equals(OBJECTARRAY))
      block.add("in.readArray($N)", classloader);
    else if (type.equals(INTARRAY))
      block.add("in.createIntArray()");
    else if (type.equals(LONGARRAY))
      block.add("in.createLongArray()");
    else if (type.equals(SERIALIZABLE))
      block.add("($T) in.readSerializable()", property.type);
    else if (type.equals(PARCELABLEARRAY))
      block.add("($T) in.readParcelableArray($N)", property.type, classloader);
    else if (type.equals(SPARSEARRAY))
      block.add("in.readSparseArray($N)", classloader);
    else if (type.equals(SPARSEBOOLEANARRAY))
      block.add("in.readSparseBooleanArray()");
    else if (type.equals(BUNDLE))
      block.add("in.readBundle($N)", classloader);
    else if (type.equals(PERSISTABLEBUNDLE))
      block.add("in.readPersistableBundle($N)", classloader);
    else if (type.equals(SIZE))
      block.add("in.readSize()");
    else if (type.equals(SIZEF))
      block.add("in.readSizeF()");
    else
      block.add("($T) in.readValue($N)", property.type, classloader);

    if (property.nullable()){
      block.add(" : null");
    }

    return block.build();
  }

  public static CodeBlock writeValue(Types types, AutoValueParcelExtension.Property property, ParameterSpec out) {
    CodeBlock.Builder block = CodeBlock.builder();

    if (property.nullable()) {
      block.beginControlFlow("if ($N() == null)", property.name);
      block.addStatement("$N.writeInt(1)", out);
      block.nextControlFlow("else");
      block.addStatement("$N.writeInt(0)", out);
    }

    TypeElement element = (TypeElement) types.asElement(property.element.getReturnType());
    final TypeName type = element != null ? getParcelableType(types, element) : property.type;
    if (type.equals(STRING))
      block.add("$N.writeString($N())", out, property.name);
    else if (type.equals(TypeName.BYTE))
      block.add("$N.writeInt($N())", out, property.name);
    else if (type.equals(TypeName.INT))
      block.add("$N.writeInt($N())", out, property.name);
    else if (type.equals(TypeName.SHORT))
      block.add("$N.writeInt(((Short) $N()).intValue())", out, property.name);
    else if (type.equals(TypeName.LONG))
      block.add("$N.writeLong($N())", out, property.name);
    else if (type.equals(TypeName.FLOAT))
      block.add("$N.writeFloat($N())", out, property.name);
    else if (type.equals(TypeName.DOUBLE))
      block.add("$N.writeDouble($N())", out, property.name);
    else if (type.equals(TypeName.BOOLEAN))
      block.add("$N.writeInt($N() ? 1 : 0)", out, property.name);
    else if (type.equals(PARCELABLE))
      block.add("$N.writeParcelable($N(), 0)", out, property.name);
    else if (type.equals(CHARSEQUENCE))
      block.add("$N.writeCharSequence($N())", out, property.name);
    else if (type.equals(MAP))
      block.add("$N.writeMap($N())", out, property.name);
    else if (type.equals(LIST))
      block.add("$N.writeList($N())", out, property.name);
    else if (type.equals(BOOLEANARRAY))
      block.add("$N.writeBooleanArray($N())", out, property.name);
    else if (type.equals(BYTEARRAY))
      block.add("$N.writeByteArray($N())", out, property.name);
    else if (type.equals(STRINGARRAY))
      block.add("$N.writeStringArray($N())", out, property.name);
    else if (type.equals(CHARSEQUENCEARRAY))
      block.add("$N.writeCharSequenceArray($N())", out, property.name);
    else if (type.equals(IBINDER))
      block.add("$N.writeStrongBinder($N())", out, property.name);
    else if (type.equals(OBJECTARRAY))
      block.add("$N.writeArray($N())", out, property.name);
    else if (type.equals(INTARRAY))
      block.add("$N.writeIntArray($N())", out, property.name);
    else if (type.equals(LONGARRAY))
      block.add("$N.writeLongArray($N())", out, property.name);
    else if (type.equals(SERIALIZABLE))
      block.add("$N.writeSerializable($N())", out, property.name);
    else if (type.equals(PARCELABLEARRAY))
      block.add("$N.writeParcelableArray($N())", out, property.name);
    else if (type.equals(SPARSEARRAY))
      block.add("$N.writeSparseArray($N())", out, property.name);
    else if (type.equals(SPARSEBOOLEANARRAY))
      block.add("$N.writeSparseBooleanArray($N())", out, property.name);
    else if (type.equals(BUNDLE))
      block.add("$N.writeBundle($N())", out, property.name);
    else if (type.equals(PERSISTABLEBUNDLE))
      block.add("$N.writePersistableBundle($N())", out, property.name);
    else if (type.equals(SIZE))
      block.add("$N.writeSize($N())", out, property.name);
    else if (type.equals(SIZEF))
      block.add("$N.writeSizeF($N())", out, property.name);
    else
      block.add("$N.writeValue($N())", out, property.name);

    block.add(";\n");

    if (property.nullable()) {
      block.endControlFlow();
    }

    return block.build();
  }
}
