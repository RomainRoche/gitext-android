import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.gitext.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.gitext.sample"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Reads from local.properties or falls back to -PGITEXT_API_KEY gradle flag
        val localProps = Properties().also { props ->
            rootProject.file("local.properties").takeIf { it.exists() }
                ?.inputStream()?.use { stream -> props.load(stream) }
        }
        val apiKey = localProps.getProperty("GITEXT_API_KEY")
            ?: project.findProperty("GITEXT_API_KEY") as String?
            ?: ""
        val baseUrl = localProps.getProperty("GITEXT_BASE_URL")
            ?: project.findProperty("GITEXT_BASE_URL") as String?
            ?: ""
        buildConfigField("String", "GITEXT_API_KEY", "\"$apiKey\"")
        buildConfigField("String", "GITEXT_BASE_URL", "\"$baseUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":sdk"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.android)
    implementation(libs.appcompat)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)
}
