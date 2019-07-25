# Change Log

## Version 0.2.7 (2019-07-25)

Supports: AutoValue 1.6.5

* Add SuppressWarnings for all parameterized types. (#142)
* Emit @Generated annotations (#139)
* Shade autocommon and guava dependencies (#135)
* Update dependencies, fix master, and support incremental processing (#133)

## Version 0.2.6 (2017-12-06)

Supports: AutoValue 1.5.2

* Use Enum.valueOf instead of per-enum valueOf method. (#97) 
* Explicitly call name() on the Enum type instead of the implementation. (#98)
* Generate Nullable annotation at constructor arguments. (#124)
* Update from apt to annotationProcessor

## Version 0.2.5 (2016-10-18)

Supports AutoValue 1.3

* Updates extension to support AutoValue 1.3
* Adds support for null checks for Parcel methods documented to handle null (fixes #85)
* Write unboxed short directly with writeInt. (fixes #87)
* Adds support for generics with upper bound of any Parcelable type. (fixes #31)

## Version 0.2.4-rc2 (2016-08-23)

Supports: AutoValue 1.3-rc2

* Fixes issue causing multiple levels of nested generics to generate broken code.

## Version 0.2.3-rc2 (2016-06-15)

Supports: AutoValue 1.3-rc2

* Updates extension to support AutoValue 1.3-rc2
* Fixes issue causing incorrect ClassLoader to be used 

## Version 0.2.3-rc1 (2016-06-13)

Supports: AutoValue 1.3-rc1

* Updates extension to support AutoValue 1.3-rc1
* Fixes issue when using multiple TypeAdapters of the same name
* Uses constant TypeAdapters instead of recreating them for each type
* Fix for `@Nullable` properties with TypeAdapter (fixes #66) 

## Version 0.2.2 (2016-05-26)

Supports: AutoValue 1.2

* Adds SuppressWarnings annotaiton to createFromParcel
* Fail when Creator or writeToParcel are manually implemented
* Coerce boxed instances to their primitive values for serialization.
* Use TextUtils for CharSequence serialization.
* Remove CharSequence[] support as it's methods are private.
* Add support for char and char[] types.
* Omit casts when not needed.

## Version 0.2.1 (2016-03-24)

Only guaranteed to support AutoValue 1.2-rc1

* Fixes issue when using multiple TypeAdapters of the same type.
* Fixes issue when Parcelable properties don't directly inherit from Parcelable.

## Version 0.2.0 (2016-03-21)

Initial release. Only guaranteed to support AutoValue 1.2-rc1
