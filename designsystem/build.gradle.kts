plugins {
    alias(libs.plugins.android.library)
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
}

dependencies {
    // 폰(material3)과 워치(wear material3)가 같이 쓰도록 Material 이 아닌 Compose UI 타입만 쓴다.
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
}
