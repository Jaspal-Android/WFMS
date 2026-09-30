# Rules for the minified release build (isMinifyEnabled = true).
# Retrofit, Gson, Hilt and Parcelize ship their own consumer rules; only project-specific
# needs belong here.

# Keep line numbers so Crashlytics stack traces stay readable, but hide the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
