plugins {
    alias(libs.plugins.android.application)${KOTLIN_PLUGIN}
}

android {
    namespace = "${PACKAGE}"
    compileSdk = ${COMPILE_SDK}

    defaultConfig {
        applicationId = "${PACKAGE}"
        minSdk = ${MIN_SDK}
        targetSdk = ${TARGET_SDK}
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Optional release signing config:
            // storeFile = file("../release.keystore")
            // storePassword = "android"
            // keyAlias = "androidreleasekey"
            // keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
${KOTLIN_OPTIONS}${BUILD_FEATURES}${COMPOSE_OPTIONS}
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
${COMPOSE_DEPS}${VIEWS_DEPS}${NAV_DEPS}${CATEGORY_DEPS}    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
