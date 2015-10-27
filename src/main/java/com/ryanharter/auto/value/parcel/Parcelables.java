package com.ryanharter.auto.value.parcel;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

/**
 * Created by rharter on 10/20/15.
 */
public final class Parcelables {

  private static final TypeName STRING = ClassName.get("java.lang", "String");
  private static final TypeName MAP = ClassName.get("java.util", "Map");
  private static final TypeName LIST = ClassName.get("java.util", "List");
  private static final TypeName BOOLEANARRAY = ClassName.get(boolean[].class);
  private static final TypeName BYTEARRAY = ClassName.get(byte[].class);
  private static final TypeName INTARRAY = ClassName.get(int[].class);
  private static final TypeName LONGARRAY = ClassName.get(long[].class);
  private static final TypeName STRINGARRAY = ClassName.get(String[].class);
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

  public static CodeBlock readValue(AutoValueParcelExtension.Property property, ParameterSpec in,
      FieldSpec classloader) {
    CodeBlock.Builder block = CodeBlock.builder();
    final TypeName type = property.type;
    if (type == STRING)
      block.add("$N.readString()", in);
    else if (type == TypeName.BYTE)
      block.add("$N.readByte()", in);
    else if (type == TypeName.INT)
      block.add("$N.readInt()", in);
    else if (type == TypeName.SHORT)
      block.add("(short) $N.readInt()", in);
    else if (type == TypeName.LONG)
      block.add("$N.readLong()", in);
    else if (type == TypeName.FLOAT)
      block.add("$N.readFloat()", in);
    else if (type == TypeName.DOUBLE)
      block.add("$N.readDouble()", in);
    else if (type == TypeName.BOOLEAN)
      block.add("$N.readInt() == 1", in);
    else if (type == PARCELABLE)
      block.add("$N.readParcelable($N)", in, classloader);
    else if (type == CHARSEQUENCE)
      block.add("$N.readCharSequence()", in);
    else if (type == MAP)
      block.add("$N.readHashMap($N)", in, classloader);
    else if (type == LIST)
      block.add("$N.readArrayList($N)", in, classloader);
    else if (type == BOOLEANARRAY)
      block.add("$N.createBooleanArray()", in);
    else if (type == BYTEARRAY)
      block.add("$N.createByteArray()", in);
    else if (type == STRINGARRAY)
      block.add("$N.readStringArray()", in);
    else if (type == CHARSEQUENCEARRAY)
      block.add("$N.readCharSequenceArray()", in);
    else if (type == IBINDER)
      block.add("$N.readStrongBinder()", in);
    else if (type == OBJECTARRAY)
      block.add("$N.readArray($N)", in, classloader);
    else if (type == INTARRAY)
      block.add("$N.createIntArray()", in);
    else if (type == LONGARRAY)
      block.add("$N.createLongArray()", in);
    else if (type == SERIALIZABLE)
      block.add("$N.readSerializable($N)", in, classloader);
    else if (type == PARCELABLEARRAY)
      block.add("$N.readParcelableArray($N)", in, classloader);
    else if (type == SPARSEARRAY)
      block.add("$N.readSparseArray($N)", in, classloader);
    else if (type == SPARSEBOOLEANARRAY)
      block.add("$N.readSparseBooleanArray()", in);
    else if (type == BUNDLE)
      block.add("$N.readBundle($N)", in, classloader);
    else if (type == PERSISTABLEBUNDLE)
      block.add("$N.readPersistableBundle($N)", in, classloader);
    else if (type == SIZE)
      block.add("$N.readSize()", in);
    else if (type == SIZEF)
      block.add("$N.readSizeF()", in);
    else
      block.add("($T) $N.readValue($N)", property.type, in, classloader);
    return block.build();
  }

  public static CodeBlock writeValue(AutoValueParcelExtension.Property property, ParameterSpec out) {
    CodeBlock.Builder block = CodeBlock.builder();
    final TypeName type = property.type;
    if (type == STRING)
      block.add("$N.writeString($N())", out, property.name);
    else if (type == TypeName.BYTE)
      block.add("$N.writeInt($N())", out, property.name);
    else if (type == TypeName.INT)
      block.add("$N.writeInt($N())", out, property.name);
    else if (type == TypeName.SHORT)
      block.add("$N.writeInte(((Short) $N()).intValue())", out, property.name);
    else if (type == TypeName.LONG)
      block.add("$N.writeLong($N())", out, property.name);
    else if (type == TypeName.FLOAT)
      block.add("$N.writeFloat($N())", out, property.name);
    else if (type == TypeName.DOUBLE)
      block.add("$N.writeDouble($N())", out, property.name);
    else if (type == TypeName.BOOLEAN)
      block.add("$N.writeInt($N() ? 1 : 0)", out, property.name);
    else if (type == PARCELABLE)
      block.add("$N.writeParcelable($N(), 0)", out, property.name);
    else if (type == CHARSEQUENCE)
      block.add("$N.writeCharSequence($N())", out, property.name);
    else if (type == MAP)
      block.add("$N.writeMap($N())", out, property.name);
    else if (type == LIST)
      block.add("$N.writeList($N())", out, property.name);
    else if (type == BOOLEANARRAY)
      block.add("$N.writeBooleanArray($N())", out, property.name);
    else if (type == BYTEARRAY)
      block.add("$N.writeByteArray($N())", out, property.name);
    else if (type == STRINGARRAY)
      block.add("$N.writeStringArray($N())", out, property.name);
    else if (type == CHARSEQUENCEARRAY)
      block.add("$N.writeCharSequenceArray($N())", out, property.name);
    else if (type == IBINDER)
      block.add("$N.writeStrongBinder($N())", out, property.name);
    else if (type == OBJECTARRAY)
      block.add("$N.writeArray($N())", out, property.name);
    else if (type == INTARRAY)
      block.add("$N.writeIntArray($N())", out, property.name);
    else if (type == LONGARRAY)
      block.add("$N.writeLongArray($N())", out, property.name);
    else if (type == SERIALIZABLE)
      block.add("$N.writeSerializable($N())", out, property.name);
    else if (type == PARCELABLEARRAY)
      block.add("$N.writeParcelableArray($N())", out, property.name);
    else if (type == SPARSEARRAY)
      block.add("$N.writeSparseArray($N())", out, property.name);
    else if (type == SPARSEBOOLEANARRAY)
      block.add("$N.writeSparseBooleanArray($N())", out, property.name);
    else if (type == BUNDLE)
      block.add("$N.writeBundle($N())", out, property.name);
    else if (type == PERSISTABLEBUNDLE)
      block.add("$N.writePersistableBundle($N())", out, property.name);
    else if (type == SIZE)
      block.add("$N.writeSize($N())", out, property.name);
    else if (type == SIZEF)
      block.add("$N.writeSizeF($N())", out, property.name);
    else
      block.add("$N.writeValue($N())", out, property.name);
    return block.build();
  }
}
