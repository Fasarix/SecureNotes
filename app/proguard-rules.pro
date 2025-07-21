# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Mantieni classi di AndroidX
-keep class androidx.** { *; }

# Mantieni classi usate dalla Biometric API
-keep class androidx.biometric.** { *; }

# Mantieni entità Room
-keep class com.yourpackage.model.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Mantieni classi per encrypted preferences/file
-keep class androidx.security.crypto.** { *; }

# Mantieni entry point (Application)
-keep class com.example.securenotes.ui.auth.WelcomeActivity{ *; }

# Mantieni classi usate dinamicamente (Gson)
-keepnames class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Suppress auto-generated warnings
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
