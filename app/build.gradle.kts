import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.drdisagree.iconify"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.drdisagree.iconify"
        minSdk = 31
        targetSdk = 35
        versionCode = 25
        versionName = "7.3.0"
        multiDexEnabled = true
        buildConfigField("int", "MIN_SDK_VERSION", "$minSdk")
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    var releaseSigning = signingConfigs.getByName("debug")

    try {
        val keystoreProperties = Properties()
        FileInputStream(keystorePropertiesFile).use { inputStream ->
            keystoreProperties.load(inputStream)
        }

        releaseSigning = signingConfigs.create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
            storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
        }
    } catch (_: Exception) {
    }

    // Stable debug key shared by local and CI builds so consecutive
    // debug APKs keep the same signature (ephemeral runner keys
    // previously forced an uninstall on every update).
    var debugSigning = signingConfigs.getByName("debug")

    try {
        val stableDebugKeystore = rootProject.file("app/debug.keystore")
        check(stableDebugKeystore.exists())

        debugSigning = signingConfigs.create("stableDebug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = stableDebugKeystore
            storePassword = "android"
        }
    } catch (_: Exception) {
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isCrunchPngs = false
            resValue("string", "derived_app_name", "Circle Battery")
            signingConfig = debugSigning
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            resValue("string", "derived_app_name", "Circle Battery")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = releaseSigning
        }
    }

    if (hasProperty("splitApks")) {
        splits {
            abi {
                isEnable = true
                reset()
                // Optional: -PtargetAbi=arm64-v8a to build a single ABI only.
                val targetAbi = findProperty("targetAbi") as String?
                if (targetAbi != null) include(targetAbi)
                else include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                isUniversalApk = targetAbi == null
            }
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs.excludes += setOf(
            "/META-INF/*",
            "/META-INF/versions/**",
            "/org/bouncycastle/**",
            "/kotlin/**",
            "/kotlinx/**"
        )

        resources.excludes += setOf(
            "/META-INF/*",
            "/META-INF/versions/**",
            "/org/bouncycastle/**",
            "/kotlin/**",
            "/kotlinx/**",
            "rebel.xml",
            "/*.txt",
            "/*.bin",
            "/*.json"
        )

        jniLibs.useLegacyPackaging = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-deprecation")
}

gradle.taskGraph.whenReady {
    gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS
    gradle.startParameter.warningMode = WarningMode.Summary
}

// Dependencies block

dependencies {
    // Kotlin
    implementation(libs.androidx.core.ktx)

    // Core Library Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Data Binding
    implementation(libs.library)

    // Xposed API
    // F-Droid disallow `api.xposed.info` since it's not a "Trusted Maven Repository".
    // So we create a mirror GitHub repository and obtain the library from `jitpack.io` instead.
    // Equivalent to `implementation 'de.robv.android.xposed:api:82'`.
    compileOnly(libs.xposedbridge)

    // The core module that provides APIs to a shell
    implementation(libs.su.core)
    // Optional: APIs for creating root services. Depends on ":core"
    implementation(libs.su.service)
    // Optional: Provides remote file system support
    implementation(libs.su.nio)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Material Components
    implementation(libs.material)

    // Remote Preference
    implementation(libs.remotepreferences)

    // Misc
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.interpolator)
}

tasks.register("printVersionName") {
    println(android.defaultConfig.versionName?.replace("-(Stable|Beta)".toRegex(), ""))
}