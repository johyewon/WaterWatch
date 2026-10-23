plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hanix.waterwatch.designsystem"
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    // 폰(material3)과 워치(wear material3)가 같이 쓰도록 Material 은 쓰지 않는다.
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
}
