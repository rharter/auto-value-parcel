package com.ryanharter.autoparcels;

import com.google.auto.value.AutoValueExtension;

import javax.lang.model.element.TypeElement;

/**
 * Created by rharter on 5/1/15.
 */
public class AutoParcelsExtension implements AutoValueExtension {
  @Override
  public boolean applicable(Context context) {
    TypeElement parcelable = context.processingEnvironment().getElementUtils().getTypeElement("android.os.Parcelable");
    return parcelable != null &&
        context.processingEnvironment().getTypeUtils().isAssignable(context.autoValueClass().asType(), parcelable.asType());
  }

  @Override
  public String generateClass(Context context, String s, String s1, String s2) {
    return "WOOT WOOT";
  }
}
