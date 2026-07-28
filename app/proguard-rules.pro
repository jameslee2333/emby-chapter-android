# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.embychapter.**$$serializer { *; }
-keepclassmembers class com.embychapter.** {
    *** Companion;
}
-keepclasseswithmembers class com.embychapter.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Glance AppWidget
# Glance 框架通过反射与 Compose 运行时调用 widget 类与 @Composable 函数，
# R8 full mode 会混淆/删除这些类，导致 release 包小组件加载失败，需显式保留。
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# 应用 widget 类与 @Composable 函数（Compose 编译器生成的辅助代码不可被裁剪）
-keep class com.embychapter.widget.** { *; }
-keepclasseswithmembers class com.embychapter.** {
    @androidx.compose.runtime.Composable <methods>;
}
