package android.os;

public interface Parcel {
  byte readByte();
  int readInt();
  long readLong();
  float readFloat();
  double readDouble();

  void writeInt(int in);

}