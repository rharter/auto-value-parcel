package android.os;

public interface Parcelable {
  public interface Creator<T> {
    T createFromParcel(Parcel var1);

    T[] newArray(int var1);
  }

  int describeContents();
  void writeToParcel(Object dest, int flags);

}
