plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "de.nexus.app"
    compileSdk = 34
    defaultConfig { applicationId = "de.nexus.app"; minSdk = 26; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging {
        resources {
            excludes += listOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/*.version")
        }
    }
}
dependencies {
    implementation("com.google.mediapipe:tasks-genai:0.10.24")
    implementation("org.mozilla:rhino:1.7.14")
    implementation("com.google.mlkit:image-labeling:17.0.8")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.json:json:20240303")
    implementation("com.alphacephei:vosk-android:0.3.47")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
}
