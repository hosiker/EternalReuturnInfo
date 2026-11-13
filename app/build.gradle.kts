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

    // 실험체 이미지(얼굴·전신 그림, 팬키트 자료) 포함 여부. 기본은 "제외"(이니셜 아바타로 표시)이고,
    // 님블뉴런의 사용 허락을 받은 뒤 local.properties 에 useCharacterImages=true 를 넣으면 포함한다.
    // 제외하면 이미지 파일이 앱(APK)에도 들어가지 않는다. 이미지는 app/src/characterImages/res 에 있다.
    val useCharacterImages = localProperties.getProperty("useCharacterImages") == "true"
    logger.lifecycle("실험체 이미지: " + if (useCharacterImages) "포함" else "제외 (이니셜 아바타)")

    defaultConfig {
        // Play 스토어에 등록된 패키지명. 소스 패키지(namespace, com.irionna)와 다르지만 의도된 값이라 바꾸지 않는다.
        // 이 값을 바꾸면 Play 업데이트가 거부되고 google-services.json 도 다시 받아야 한다.
        applicationId = "com.erionna.eternalreturninfo"
        minSdk = 24
        targetSdk = 36
        versionCode = 26
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "google_Token",localProperties.getProperty("googleToken"))
        buildConfigField("String", "MAIN_APIKEY", localProperties.getProperty("mainAPIKEY"))
        buildConfigField("String", "YOUTUBE_APIKEY", localProperties.getProperty("youtube_api_key"))
        buildConfigField("boolean", "USE_CHARACTER_IMAGES", useCharacterImages.toString())
    }

    sourceSets {
        if (useCharacterImages) {
            getByName("main").res.srcDir("src/characterImages/res")
        }
    }

    // 배포용 서명(Play 업로드 키). 키 파일과 비밀번호는 저장소에 올리지 않고 local.properties 에만 둔다:
    //   uploadStoreFile=C\\:\\dev\\keys\\er-info-upload.jks
    //   uploadStorePassword=...   uploadKeyAlias=...   uploadKeyPassword=...
    // 네 값이 모두 있을 때만 release 에 적용한다. 만드는 방법은 store-assets/release-checklist.md 참고.
    val uploadStoreFile = localProperties.getProperty("uploadStoreFile")
    val uploadStorePassword = localProperties.getProperty("uploadStorePassword")
    val uploadKeyAlias = localProperties.getProperty("uploadKeyAlias")
    val uploadKeyPassword = localProperties.getProperty("uploadKeyPassword")
    val hasUploadKey = listOf(uploadStoreFile, uploadStorePassword, uploadKeyAlias, uploadKeyPassword).all { !it.isNullOrBlank() }
    if (hasUploadKey) {
        signingConfigs {
            create("upload") {
                storeFile = file(uploadStoreFile!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasUploadKey) {
                signingConfig = signingConfigs.getByName("upload")
            }
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
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // CircleView
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")

    // Converter ( JSON 타입 결과를 객체로 매핑 )
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // okhttp3
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Fragment / Activity (ViewModel 등)
    implementation("androidx.fragment:fragment-ktx:1.9.1")
    implementation("androidx.activity:activity-ktx:1.13.0")

    // SplashScreen
    implementation("androidx.core:core-splashscreen:1.2.0")

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


    // reCaptcha for firebase auth issue
    implementation("com.google.android.recaptcha:recaptcha:18.4.0")
}
