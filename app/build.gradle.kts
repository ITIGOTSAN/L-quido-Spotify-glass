plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.personal.spotifyglass"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.personal.spotifyglass"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "2.0"
    }
    buildFeatures { compose = true }
    packaging {
        resources {
            excludes += arrayOf(
                "DebugProbesKt.bin", "kotlin-tooling-metadata.json", "kotlin/**",
                "META-INF/*.version", "META-INF/**/LICENSE.txt"
            )
        }
        dex { useLegacyPackaging = true }
        jniLibs { useLegacyPackaging = true }
    }
    lint { checkReleaseBuilds = false }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("org.jetbrains.compose.foundation:foundation:1.12.0")
    implementation("org.jetbrains.compose.ui:ui:1.12.0")
    implementation("org.jetbrains.compose.ui:ui-graphics:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.github.kyant0:shapes:1.2.1")
    implementation("io.github.kyant0:backdrop:2.0.1")
    compileOnly("io.github.libxposed:api:102.0.0")
}
