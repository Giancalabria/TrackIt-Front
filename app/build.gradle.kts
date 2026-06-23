plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

fun readEnvFile(rootDir: File): Map<String, String> {
    val envFile = rootDir.resolve(".env")
    if (!envFile.exists()) return emptyMap()
    return envFile.readLines()
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filterNot { it.startsWith("#") }
        .mapNotNull { line ->
            val idx = line.indexOf("=")
            if (idx <= 0) return@mapNotNull null
            val key = line.substring(0, idx).trim()
            val value = line.substring(idx + 1).trim()
            key to value
        }
        .toMap()
}

android {
    namespace = "com.trackit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.trackit"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val env = readEnvFile(rootProject.projectDir)
        val orsKey = env["ORS_API_KEY"].orEmpty()
        buildConfigField("String", "ORS_API_KEY", "\"$orsKey\"")

        val supabaseUrl = env["SUPABASE_URL"].orEmpty()
        val supabaseAnonKey = env["SUPABASE_ANON_KEY"].orEmpty()
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.23")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.23")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.23")
        // 16 KB page size alignment: keep CameraX/ML Kit native libs at versions that
        // ship 16 KB-aligned .so files even if a transitive dep requests older ones.
        force("androidx.camera:camera-core:1.4.2")
        force("androidx.camera:camera-camera2:1.4.2")
        force("androidx.camera:camera-lifecycle:1.4.2")
        force("androidx.camera:camera-view:1.4.2")
        force("com.google.mlkit:barcode-scanning:17.3.0")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Room (local persistence / offline single source of truth)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager (background sync to Supabase)
    implementation(libs.androidx.work.runtime.ktx)

    // DataStore (onboarding completion flags)
    implementation(libs.androidx.datastore.preferences)

    // Maps and Networking
    implementation(libs.osmdroid)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Supabase 2.x (Kotlin 1.9 + Ktor 2.3)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit Barcode Scanning
    implementation(libs.barcode.scanning)
    implementation(libs.guava)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
}
