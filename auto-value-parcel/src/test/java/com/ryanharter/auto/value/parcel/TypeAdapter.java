package com.ryanharter.auto.value.parcel;

import android.os.Parcel;

public interface TypeAdapter<T> {

  T fromParcel(Parcel in);

  void toParcel(T value, Parcel dest);

}
