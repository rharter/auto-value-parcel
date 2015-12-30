package com.ryanharter.auto.value.parcel;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * An annotation that indicates the auto-value-parcel {@link TypeAdapter} to use to
 * parcel and unparcel the field.  The value must be set to a valid {@link TypeAdapter}
 * class.
 *
 * <pre>
 * <code>
 * {@literal @}AutoValue public abstract class Foo extends Parcelable {
 *   {@literal @}ParcelAdapter(DateTypeAdapter.class) public abstract Date date();
 * }
 * </code>
 * </pre>
 *
 * The generated code will instantiate and use the {@code DateTypeAdapter} class to parcel and
 * unparcel the {@code date()} property. In order for the generated code to instantiate the
 * {@link TypeAdapter}, it needs a public, no-arg constructor.
 */
@Target(METHOD)
@Retention(SOURCE)
@Documented
public @interface ParcelAdapter {
  Class<? extends TypeAdapter<?>> value();
}
