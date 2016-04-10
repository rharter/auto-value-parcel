package com.ryanharter.auto.value.parcel.model;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class SampleTypeWithParcelableContractSatisfied implements Parcelable {
  public static final Parcelable.Creator<SampleTypeWithParcelableContractSatisfied> CREATOR =
      new Parcelable.Creator<SampleTypeWithParcelableContractSatisfied>() {
        @Override
        public SampleTypeWithParcelableContractSatisfied createFromParcel(Parcel var1) {
          return null;
        }

        @Override
        public SampleTypeWithParcelableContractSatisfied[] newArray(int var1) {
          return new SampleTypeWithParcelableContractSatisfied[0];
        }
      };

  @Override
  public void writeToParcel(Parcel dest, int flags) {
  }

  @Override
  public int describeContents() {
    return 0;
  }
}
