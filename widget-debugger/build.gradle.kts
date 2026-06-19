plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.morainet.widget.debugger"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":widget-core"))
    api(libs.glance.appwidget)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(platform(libs.androidx.compose.bom))

    testImplementation(libs.junit)
}

// Maven 发布
apply(from = "../publish.gradle.kts")
