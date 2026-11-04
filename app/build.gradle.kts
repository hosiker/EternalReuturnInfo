import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("kotlin-android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.irionna.eternalreturninfo"
    compileSdk = 36

    val localProperties = Properties()
    localProperties.load(FileInputStream(rootProject.file("local.properties")))

    defaultConfig {
        applicationId = "com.irionna.eternalreturninfo"
        minSdk = 24
        targetSdk = 33
        versionCode = 25
        versionName = "1.25"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "google_Token",localProperties.getProperty("googleToken"))
        buildConfigField("String", "MAIN_APIKEY", localProperties.getProperty("mainAPIKEY"))
        buildConfigField("String", "YOUTUBE_APIKEY", localProperties.getProperty("youtube_api_key"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // CircleView
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Converter ( JSON 타입 결과를 객체로 매핑 )
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // okhttp3
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Fragment / Activity (ViewModel 등)
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // SplashScreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Google 로그인 (Google Play services)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Coil
    implementation("io.coil-kt:coil:0.10.0")

    // firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // swiperefreshlayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // spinner
    implementation("com.github.skydoves:powerspinner:1.2.7")

    // powerMenu
    implementation("com.github.skydoves:powermenu:2.2.4")

    // MotionToast
    implementation("com.github.Spikeysanju:MotionToast:1.4")

    // reCaptcha for firebase auth issue
    implementation("com.google.android.recaptcha:recaptcha:18.4.0")
}
