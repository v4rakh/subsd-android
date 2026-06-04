plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.protobuf")
}

android {
    namespace = "de.varakh.subsd"
    compileSdk = 36
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "de.varakh.subsd"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.2.1"
    }

    // Release signing — reads from env vars set via fastlane/secrets/signing.env
    val keystoreFile = System.getenv("SUBSD_KEYSTORE_FILE")
    val keystorePassword = System.getenv("SUBSD_KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("SUBSD_KEY_ALIAS")
    val keyPassword = System.getenv("SUBSD_KEY_PASSWORD")

    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = rootProject.file(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    lint {
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.29.1"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.69.1"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.3:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc") { option("lite") }
                create("grpckt") { option("lite") }
            }
            task.builtins {
                create("java") { option("lite") }
                create("kotlin") { option("lite") }
            }
            // Workaround: CI cache restore strips execute permissions from protoc plugin binaries
            task.doFirst {
                fileTree(gradle.gradleUserHomeDir) {
                    include("caches/modules-2/files-2.1/io.grpc/**/*.exe")
                    include("caches/modules-2/files-2.1/com.google.protobuf/**/*.exe")
                }.forEach { f -> f.setExecutable(true) }
            }
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ExoPlayer + MediaSession (required for mediaPlayback foreground service type on API 35+)
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-session:1.6.1")

    // Queue drag-and-drop reordering
    implementation("sh.calvin.reorderable:reorderable:2.4.3")

    // Networking (REST + WS)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20231013")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Image loading (uses same OkHttp client for auth cookies)
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")

    // Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // gRPC (satellite protocol)
    implementation("io.grpc:grpc-kotlin-stub:1.4.3")
    implementation("io.grpc:grpc-okhttp:1.69.1")
    implementation("io.grpc:grpc-protobuf-lite:1.69.1")
    implementation("com.google.protobuf:protobuf-kotlin-lite:4.29.1")
}
