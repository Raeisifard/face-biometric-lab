plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.isc.face_mobile_demo"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        applicationId = "com.isc.face_mobile_demo"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    val cameraXVersion = "1.4.2"
    val onnxRuntimeVersion = "1.25.0"
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:$onnxRuntimeVersion")
}

val syncYuNetModel by tasks.registering(Copy::class) {
    val source = rootProject.projectDir.resolve("../models/detector/face_detection_yunet_2023mar.onnx")
    from(source)
    into(layout.projectDirectory.dir("src/main/assets/models"))
    onlyIf { source.exists() }
}

tasks.named("preBuild") {
    dependsOn(syncYuNetModel)
}

flutter {
    source = "../.."
}
