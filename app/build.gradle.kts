plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.moneylog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.moneylog"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations += listOf("ko", "en", "ja")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Java 8+ API (LocalDate 등) 역호환성
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    // --- Java 8+ 역호환 (LocalDate 등) ---
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // --- Jetpack Core ---
    implementation(libs.appcompat)
    implementation(libs.fragment)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)

    // --- Lifecycle ---
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)

    // --- Navigation ---
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // --- Room (SQLite ORM) ---
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // --- Hilt (DI) ---
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    implementation(libs.hilt.work)
    annotationProcessor(libs.hilt.work.compiler)

    // --- WorkManager (반복 거래 자동화) ---
    implementation(libs.work.runtime)

    // --- 보안 (PIN 해시, 생체인증) ---
    implementation(libs.security.crypto)
    implementation(libs.biometric)

    // --- Google Drive 백업 ---
    implementation(libs.google.api.client.android) {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation(libs.google.api.services.drive) {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation(libs.google.play.services.auth)

    // --- Gemini Nano (온디바이스 AI) — 향후 구현, 현재 비활성 ---
    // implementation(libs.google.ai.edge)

    // --- AdMob (Phase 7 — 현재 비활성) ---
    // implementation(libs.admob)

    // --- 단위 테스트 ---
    testImplementation(libs.junit)
    testImplementation(libs.room.testing)

    // --- Instrumented 테스트 ---
    androidTestImplementation(libs.junit.android.ext)
    androidTestImplementation(libs.espresso.core)
}
