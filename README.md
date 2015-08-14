# AutoValue: Parcel Extension

[![Build Status](https://travis-ci.org/rharter/auto-parcel.svg?branch=master)](https://travis-ci.org/rharter/auto-parcel)

An extension for Google's [AutoValue](https://github.com/google/auto) that supports Android's Parcelable interface.

**Note**: This is an early version that requires the extension support currently in AutoValue 1.2-SNAPSHOT.

## Usage

Simply include AutoParcel in your project and make any of your `@AutoValue` annotated classed implement `Parcelable`.

```java
@AutoValue public abstract class Foo implements Parcelable {

  public abstract String bar();
  
  // needed workaround for now. 
  @Override public int describeContents() {
    return 0;
  }
}
```

Now build your project and enjoy your Parcelable Foo.

## Download

Add a Gradle dependency:

```groovy
compile 'com.ryanharter.auto-parcel:auto-parcel:0.2-SNAPSHOT'
```

## License

```
Copyright 2015 Ryan Harter.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
