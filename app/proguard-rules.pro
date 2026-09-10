# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.exchangerates.app.**$$serializer { *; }
-keepclassmembers class com.exchangerates.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.exchangerates.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions
