plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.openhealthbridge.core.models"
    compileSdk = 35
    defaultConfig { minSdk = 28 }
}
