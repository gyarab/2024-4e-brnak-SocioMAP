plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.example.sociomap2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sociomap2"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.play.services.auth)
    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    //implementation(libs.firebase.auth.ktx)

    // Google Play Services Maps
    implementation (libs.play.services.maps.v1802)

    // Other dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
    implementation (libs.play.services.maps)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.auth.v2231)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.core.ktx)
    implementation(libs.googleid)
    //noinspection UseTomlInstead
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //Email sending
    implementation (libs.android.mail)
    implementation (libs.mail.android.activation)
}

apply(plugin = "com.google.gms.google-services")
