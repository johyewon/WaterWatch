plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hanix.waterwatch.common"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
