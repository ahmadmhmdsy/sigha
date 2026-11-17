import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Release signing comes from a git-ignored keystore/keystore.properties.
// The repository ships no key material: the publisher's Play upload key is theirs alone.
val keystorePropsFile = rootProject.file("keystore/keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val hasReleaseKey = keystorePropsFile.exists() &&
    !keystoreProps.getProperty("storePassword").isNullOrBlank() &&
    !keystoreProps.getProperty("keyAlias").isNullOrBlank()

android {
    namespace = "com.sigha.converter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sigha.converter"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.1"
        vectorDrawables { useSupportLibrary = true }
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file("keystore/" + keystoreProps.getProperty("keyFile", "local-release.jks"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
        }
    }

    // Per-ABI APKs for direct distribution; the AAB keeps all ABIs for Play.
    //
    // AGP 8.9+ cannot build a bundle while ABI splits are enabled: buildReleasePreBundle
    // throws "Sequence contains more than one matching element" because R8 resource shrinking
    // writes one shrunk-resources file per split (issuetracker.google.com/issues/402800800).
    // The splits are therefore disabled for bundle builds only; Play splits the AAB by itself.
    val buildingBundle = gradle.startParameter.taskNames.any { it.lowercase().contains("bundle") }
    splits {
        abi {
            isEnable = !buildingBundle
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE.txt,NOTICE.txt,LICENSE.md,NOTICE.md}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
        // Play's 16 KB page-size rule and the packaging policy require uncompressed native libs.
        jniLibs { useLegacyPackaging = false }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        // Transitive: PDFBox -> bcpkix ships a test trust manager; the app performs no TLS
        // beyond the optional OCR pack download, which uses HttpsURLConnection defaults.
        disable += "TrustAllX509TrustManager"
    }
}

dependencies {
    // Core / lifecycle / activity
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Images
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Conversion engines (all permissive/LGPL - see NOTICE.md and docs/legal/licence-compliance.md)
    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.tukaani:xz:1.12")

    // Optional OCR model support (Tesseract4Android, Apache-2.0)
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
