# AutoValue: Parcel Extension

An extension for Google's [AutoValue](https://github.com/google/auto) that supports Android's 
Parcelable interface.

## Usage

Simply include the AutoValue: Parcel Extension in your project and make any of your `@AutoValue` 
annotated classed implement `Parcelable`.

```java
@AutoValue public abstract class Foo implements Parcelable {

  public abstract String bar();

}
```

Now build your project and enjoy your Parcelable Foo.

## TypeAdapters

Out of the box AutoValue: Parcel Extension support all of the types supported by the 
[Parcel](https://developer.android.com/reference/android/os/Parcel.html) class, but sometimes you
may need to parcel other types, like SparseArray or ArrayMap.  You can do this using a custom TypeAdapter.

TypeAdapter allows you to define custom de/serialization logic for properties by allowing you to
parcel and unparcel those properties manually.

```java
public class DateTypeAdapter implements TypeAdapter<Date> {
  public Date fromParcel(Parcel in) {
    return new Date(in.readLong());
  }
  
  public void toParcel(Date value, Parcel dest) {
    dest.writeLong(value.getTime());
  }
}
```

Once you've defined your custom TypeAdapter, using it on an AutoValue class is as simple as adding
the `ParcelAdapter` annotation to any property you'd like to be serialized with your TypeAdapter.

```java
@AutoValue public abstract class Foo implements Parcelable {
  @ParcelAdapter(DateTypeAdapter.class) public abstract Date date();
}
```

Since TypeAdapters require a small runtime component, they are optional.  To use TypeAdapters in 
your project you'll have to add a compile dependency on the `auto-value-parcel-adapter` artifact.

```groovy
compile 'com.ryanharter.auto.value:auto-value-parcel-adapter:0.2.9'
```

## Download

Add a Gradle dependency:

```groovy
annotationProcessor 'com.ryanharter.auto.value:auto-value-parcel:0.2.9'

// Optionally for TypeAdapter support
compile 'com.ryanharter.auto.value:auto-value-parcel-adapter:0.2.9'
```

([Migrating](https://bitbucket.org/hvisser/android-apt/wiki/Migration) from `apt` to `annotationProcessor`)

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
