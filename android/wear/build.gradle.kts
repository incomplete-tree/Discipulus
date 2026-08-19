import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.harrydekat.discipulus.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.harrydekat.discipulus"
        minSdk = 30
        targetSdk = 36
        versionCode = providers.gradleProperty("wearVersionCode")
            .orNull
            ?.toIntOrNull()
            ?: 22
        versionName = providers.gradleProperty("wearVersionName")
            .orNull
            ?: "0.1.9"
    }

    // Release builds use an explicitly supplied CI key when available. Local
    // builds remain installable with the standard debug key, without requiring
    // contributors to check credentials into local.properties.
    val keystorePropertiesFile: File = rootProject.file("local.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }
    val localKeystoreConfigured = listOf(
        "storeFile",
        "storePassword",
        "keyAlias",
        "keyPassword",
    ).all { keystoreProperties.containsKey(it) }
    val releaseSigningRequired = System.getenv("DISCIPULUS_RELEASE_BUILD") == "true"

    signingConfigs {
        create("release") {
            val ciKeystore = System.getenv("CM_KEYSTORE_PATH")
            val ciKeystoreConfigured = System.getenv("CI") == "true" &&
                !ciKeystore.isNullOrBlank() &&
                !System.getenv("CM_KEYSTORE_PASSWORD").isNullOrBlank() &&
                !System.getenv("CM_KEY_ALIAS").isNullOrBlank() &&
                !System.getenv("CM_KEY_PASSWORD").isNullOrBlank()
            check(!releaseSigningRequired || ciKeystoreConfigured || localKeystoreConfigured) {
                "A production release requires CM_KEYSTORE_* signing credentials."
            }

            if (ciKeystoreConfigured) {
                storeFile = file(requireNotNull(ciKeystore))
                storePassword = System.getenv("CM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CM_KEY_ALIAS")
                keyPassword = System.getenv("CM_KEY_PASSWORD")
            } else if (localKeystoreConfigured) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            val ciKeystoreConfigured = System.getenv("CI") == "true" &&
                !System.getenv("CM_KEYSTORE_PATH").isNullOrBlank() &&
                !System.getenv("CM_KEYSTORE_PASSWORD").isNullOrBlank() &&
                !System.getenv("CM_KEY_ALIAS").isNullOrBlank() &&
                !System.getenv("CM_KEY_PASSWORD").isNullOrBlank()
            signingConfig = if (ciKeystoreConfigured || localKeystoreConfigured) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "commonMain/default/manifest"
            excludes += "commonMain/default/linkdata/root_package/0_.knm"
            excludes += "nativeMain/default/linkdata/root_package/0_.knm"
            excludes += "nativeMain/default/linkdata/module"
            excludes += "nativeMain/default/manifest"
            excludes += "commonMain/default/linkdata/module"
            excludes += "META-INF/kotlin-project-structure-metadata.json"
            excludes += "commonMain/default/linkdata/**/*.knm"
            excludes += "nativeMain/default/linkdata/**/*.knm"
            pickFirsts += "META-INF/androidx/lifecycle/lifecycle-common/LICENSE.txt"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")

    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material:material-icons-extended-android:1.6.8")
    implementation("androidx.wear.compose:compose-material3:1.0.0-alpha27")
    implementation("androidx.wear.compose:compose-foundation:1.3.1")
    implementation("androidx.wear.compose:compose-navigation:1.3.1")
    implementation("androidx.compose.foundation:foundation:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
}
