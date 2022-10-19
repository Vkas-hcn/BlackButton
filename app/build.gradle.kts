plugins {
    id("com.android.application")
    id("com.google.android.gms.oss-licenses-plugin")
//    id("com.google.gms.google-services")
//    id("com.google.firebase.crashlytics")
    kotlin("android")
    id("kotlin-parcelize")
}

setupApp()

android {
    namespace = "com.github.shadowsocks"
    defaultConfig.applicationId = "com.github.shadowsocks"
}

dependencies {
    val cameraxVersion = "1.1.0"

    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("com.google.mlkit:barcode-scanning:17.0.2")
    implementation("com.google.zxing:core:3.5.0")
    implementation("com.takisoft.preferencex:preferencex-simplemenu:1.1.0")
    implementation("com.twofortyfouram:android-plugin-api-for-locale:1.0.4")
    implementation("me.zhanghai.android.fastscroll:library:1.1.8")

    implementation("com.google.android.gms:play-services-ads:21.0.0")
    implementation("com.jeremyliao:live-event-bus-x:1.5.7")
    //XUtil
    implementation ("com.github.xuexiangjys.XUtil:xutil-core:1.1.7")
    implementation("com.github.CymChad:BaseRecyclerViewAdapterHelper:3.0.8")
    implementation(platform("com.google.firebase:firebase-bom:29.3.1"))
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.tencent:mmkv:1.0.23")
    implementation ("com.airbnb.android:lottie:3.3.1")

}
