# Change Log

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
