import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("kotlin-parcelize")
}

// Load the secrets.properties file
val secrets = Properties()
file("../secrets.properties").takeIf { it.exists() }?.apply {
    secrets.load(inputStream())
}

android {
    namespace = "com.atvantiq.wfms"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.atvantiq.wfms"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["googleMapsApiKey"] = secrets.getProperty("GOOGLE_MAPS_API_KEY")
        buildConfigField("String","GOOGLE_MAPS_API_KEY","\"" + secrets.getProperty("GOOGLE_MAPS_API_KEY") + "\"")
        buildConfigField("String","PAYMENT_KEY","\"" + secrets.getProperty("PAYMENT_KEY") + "\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_URL", "\"http://69.62.85.16:8000/\"")
        }
        create("beta") {
            dimension = "environment"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            buildConfigField("String", "BASE_URL", "\"http://69.62.85.16:8000/\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"http://69.62.85.16:8000/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        dataBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.security.crypto.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Mockito
    //testImplementation("org.mockito:mockito-core:5.5.0")
    //testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
   // androidTestImplementation("org.mockito:mockito-android:5.5.0")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Coroutines Testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Hilt Testing
    testImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptTest("com.google.dagger:hilt-android-compiler:2.48")

    // Hilt
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("com.google.dagger:hilt-android:2.51.1")
    // Google Gson
    implementation("com.google.code.gson:gson:2.11.0")
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-rxjava2-adapter:1.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.facebook.stetho:stetho:1.5.1")
    implementation("com.facebook.stetho:stetho-okhttp3:1.5.1")
    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.0")

    //Navigation Controller

    // Views/Fragments integration
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    // Feature module support for Fragments
    implementation("androidx.navigation:navigation-dynamic-features-fragment:2.7.7")
    // Chart Drawings
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // Places API
    implementation(libs.play.services.location)
    //Circular Image View
    implementation("de.hdodenhof:circleimageview:3.1.0")
    //Google Maps
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    //Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    //Maps Utils
    implementation("com.google.maps.android:android-maps-utils:2.4.0")
    //Secure Pref
    implementation(libs.androidx.security.crypto.ktx)
    //Lottie Files
    implementation("com.airbnb.android:lottie:6.6.6")
    //Slider
    implementation("com.ncorti:slidetoact:0.11.0")
}

kapt {
    correctErrorTypes = true
}
