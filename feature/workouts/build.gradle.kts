plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.openhealthbridge.feature.workouts"
    compileSdk = 35
    defaultConfig { minSdk = 28 }
}

dependencies {
    implementation(project(":core:models"))
}
