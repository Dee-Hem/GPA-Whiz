# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers and source file attributes for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve standard annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room database, entities, and DAOs
-keep class androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep App Database models used in database and JSON export/import
-keep class com.deehem.gpawhiz.data.** { *; }
-keep class com.deehem.gpawhiz.service.BackupData { *; }

# Keep BroadcastReceivers and Activities registered in the Android Manifest
-keep public class com.deehem.gpawhiz.MainActivity { *; }
-keep public class com.deehem.gpawhiz.receiver.AlarmReceiver { *; }

# Keep Kotlin Coroutines internal names and ServiceLoaders if needed
-keepclassmembers class kotlinx.coroutines.** { *; }
