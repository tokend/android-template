# General reflection
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn javax.annotation.**
-keepclassmembers enum * { *; }

# Retrofit
-dontwarn org.codehaus.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Jackson
-keep class com.fasterxml.** { *; }
-keep @com.fasterxml.jackson.annotation.** class * { *; }
-dontwarn com.fasterxml.jackson.databind.**

# JSONAPI
-keepclassmembers class * { @com.github.jasminb.jsonapi.annotations.Id <fields>; }
-keep class * implements com.github.jasminb.jsonapi.ResourceIdHandler

# Wallet
# Uncomment this if you would like to decode XDRs
-keep class org.tokend.wallet.xdr.* { *; }
-dontnote org.tokend.wallet.xdr.*

# General
-keepattributes SourceFile,LineNumberTable,EnclosingMethod,Exceptions
-keep public class * extends java.lang.Exception

# Optimize
-repackageclasses
-optimizations !method/removal/parameter

# KYC state storage
-keepnames class io.tokend.template.features.kyc.model.** { *; }

# Keep JsonCreator
-keepclassmembers class * {
     @com.fasterxml.jackson.annotation.JsonCreator *;
}

# Legacy Picasso downloader
-dontwarn com.squareup.picasso.OkHttpDownloader

# ProGuard issue
# https://sourceforge.net/p/proguard/bugs/573/
-optimizations !class/unboxing/enum

# These classes are used via kotlin reflection and the keep might not be required anymore once Proguard supports
# Kotlin reflection directly.
-keep interface kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader
-keep class kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins.BuiltInsLoaderImpl
-keep class kotlin.Metadata

# class [META-INF/versions/9/module-info.class] unexpectedly contains class [module-info]
-dontwarn module-info

# Serializable objects
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
