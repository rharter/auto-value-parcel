package android.os;

public interface Parcelable {

  int describeContents();
  void writeToParcel(Object dest, int flags);

}
