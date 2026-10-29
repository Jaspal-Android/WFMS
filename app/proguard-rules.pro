# Rules for the minified release build (isMinifyEnabled = true).
# Retrofit, Gson, Hilt and Parcelize ship their own consumer rules; only project-specific
# needs belong here.

# Models are mapped to and from JSON by Gson and passed between screens as Parcelables; some are
# read by field name. Keeping them costs a few KB and rules out R8 renaming a field the server
# or a persisted value still spells the old way.
-keep class com.atvantiq.wfms.models.** { *; }

# Keep line numbers so Crashlytics stack traces stay readable, but hide the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
