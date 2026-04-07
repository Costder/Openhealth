plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.openhealthbridge.data.repository"
    compileSdk = 35
    defaultConfig { minSdk = 28 }
}

dependencies {
    implementation(project(":core:models"))
    implementation(project(":data:db"))
}
