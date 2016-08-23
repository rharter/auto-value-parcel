# Change Log

## Version 0.2.4-rc2 (2016-18-23)

Supports: autoValue 1.3-rc2

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
