# ----------------------------------------------------
# Proguard & R8 optimization rules for SproutMemo
# ----------------------------------------------------

# Keep generic signature metadata (CRITICAL for Retrofit generic type reflection)
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, Exceptions

# Retrofit 2 Keep rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp 3 Keep rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keep class okhttp3.** { *; }

# Gson Keep rules
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep all API interaction Model classes (reflection binding)
-keep class com.sproutmemo.android.data.remote.model.** { *; }
-keep class com.sproutmemo.android.data.remote.ApiService { *; }

# Room compiler generated classes
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.RoomDatabase
