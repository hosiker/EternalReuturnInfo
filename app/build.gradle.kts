import java.util.Properties
import java.io.FileInputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
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
            // 로컬 점검용: local.properties 에 signReleaseWithDebug=true 가 있으면 디버그 키로 서명해서
            // 난독화(R8)가 적용된 릴리스 빌드를 에뮬레이터에 설치해 볼 수 있다. 배포용 서명이 아니다.
            if (localProperties.getProperty("signReleaseWithDebug") == "true") {
                signingConfig = signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
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
        viewBinding = true
        buildConfig = true
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// 일부 라이브러리가 옛 kotlin-android-extensions-runtime(1.4.10)을 가져오는데,
// Kotlin 2.x 의 kotlin-parcelize-runtime 이 같은 kotlinx.android.parcel 클래스를 이미 포함해서
// "Duplicate class" 오류가 난다. 옛 런타임을 제외한다(새 런타임이 같은 클래스를 제공한다).
configurations.all {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions-runtime")
}

dependencies {

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
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
    implementation("androidx.fragment:fragment-ktx:1.9.1")
    implementation("androidx.activity:activity-ktx:1.13.0")

    // SplashScreen
    implementation("androidx.core:core-splashscreen:1.2.0")

    // Google 로그인 (Google Play services)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Coil
    implementation("io.coil-kt:coil:2.7.0")

    // firebase
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // swiperefreshlayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")

    // spinner
    implementation("com.github.skydoves:powerspinner:1.2.7")

    // powerMenu
    implementation("com.github.skydoves:powermenu:2.2.4")

    // MotionToast
    implementation("com.github.Spikeysanju:MotionToast:1.4")

    // reCaptcha for firebase auth issue
    implementation("com.google.android.recaptcha:recaptcha:18.4.0")
}
