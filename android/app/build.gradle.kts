import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "dev.harrydekat.discipulus"
    compileSdk = 36
    ndkVersion = "27.0.12077973" //flutter.ndkVersion

    compileOptions {
        // Enable core library desugaring
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "dev.harrydekat.discipulus"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = 26 //flutter.minSdkVersion
        targetSdk = 36 //flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    // Load keystore properties from local.properties
    val keystorePropertiesFile: File = rootProject.file("local.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }
    val ciKeystorePath = System.getenv("CM_KEYSTORE_PATH")
    val ciKeystoreConfigured = System.getenv("CI") == "true" &&
        !ciKeystorePath.isNullOrBlank() &&
        !System.getenv("CM_KEYSTORE_PASSWORD").isNullOrBlank() &&
        !System.getenv("CM_KEY_ALIAS").isNullOrBlank() &&
        !System.getenv("CM_KEY_PASSWORD").isNullOrBlank()
    val localKeystoreConfigured = listOf(
        "storeFile",
        "storePassword",
        "keyAlias",
        "keyPassword",
    ).all { keystoreProperties.containsKey(it) }
    val releaseSigningRequired = System.getenv("DISCIPULUS_RELEASE_BUILD") == "true"

    check(!releaseSigningRequired || ciKeystoreConfigured || localKeystoreConfigured) {
        "A production release requires CM_KEYSTORE_* signing credentials."
    }

    signingConfigs {
        create("release") {
            if (ciKeystoreConfigured) {
                storeFile = file(ciKeystorePath!!)
                storePassword = System.getenv("CM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CM_KEY_ALIAS")
                keyPassword = System.getenv("CM_KEY_PASSWORD")
            } else {
                // Load from local.properties
                if (localKeystoreConfigured) {
                    storeFile = file(keystoreProperties["storeFile"] as String)
                    storePassword = keystoreProperties["storePassword"] as String
                    keyAlias = keystoreProperties["keyAlias"] as String
                    keyPassword = keystoreProperties["keyPassword"] as String
                } else {
                    println("WARNING: local.properties is missing keystore information. Release builds will not be signed.")
                }
            }
        }
    }

    buildTypes {
        release {
            // Use a supplied release key in CI; local builds stay installable with
            // the standard debug key when no contributor keystore is configured.
            signingConfig = if (ciKeystoreConfigured || localKeystoreConfigured) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            // You can add other release-specific settings here, like minification
            isMinifyEnabled = true

        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
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

configurations.all {
    resolutionStrategy {
        force("androidx.work:work-runtime:2.8.1")
        force("androidx.work:work-runtime-ktx:2.8.1")
    }
}

dependencies {
    // Add the core library desugaring dependency
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation("com.google.mlkit:genai-prompt:1.0.0-alpha1")
    implementation("com.google.android.gms:play-services-tasks:18.0.2")
}

flutter {
    source = "../.."
}
