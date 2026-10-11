plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
}

android {
  namespace = "xyz.pepe.glacierclient.musicapp"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "xyz.pepe.glacierclient.musicapp"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    getByName("debug") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val ks = System.getenv("KEYSTORE_PATH")
      if (ks != null) {
        signingConfig = signingConfigs.create("release") {
          storeFile = file(ks)
          storePassword = System.getenv("STORE_PASSWORD")
          keyAlias = "upload"
          keyPassword = System.getenv("KEY_PASSWORD")
        }
      }
    }
    // Installable, R8-shrunk build signed like the debug build (so it updates the apps already on the phone):
    // `./gradlew assembleOptimized`. Much faster at runtime than debug (Compose and R8 optimisations on).
    create("optimized") {
      initWith(getByName("release"))
      signingConfig = getByName("debug").signingConfig
      isDebuggable = false
      matchingFallbacks += listOf("release")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    isCoreLibraryDesugaringEnabled = true
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  packaging {
    resources.excludes.add("META-INF/*")
  }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.matching { it.name.contains("AarMetadata") }.configureEach { enabled = false }

dependencies {
  coreLibraryDesugaring(libs.desugar)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.webkit)
  implementation(libs.mlkit.translate)
  implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  implementation(libs.retrofit)
  implementation(libs.converter.moshi)
  implementation(libs.okhttp)
  implementation(libs.moshi)
  implementation(libs.reorderable)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.datasource.okhttp)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)
  implementation(libs.androidx.palette)
  implementation(libs.androidx.datastore)
  ksp(libs.androidx.room.compiler)
  ksp(libs.moshi.codegen)

  testImplementation(platform(libs.androidx.compose.bom))
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.junit)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.test.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
