plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.openhealthbridge.data.db"
    compileSdk = 35
    defaultConfig { minSdk = 28 }
}

dependencies {
    implementation(project(":core:models"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
