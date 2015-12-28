package com.ryanharter.auto.value.parcel;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Target(METHOD)
@Retention(SOURCE)
@Documented
public @interface ParcelAdapter {
  Class value();
}
