import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun demoConfiguration(gradleProperty: String, localProperty: String): String =
    providers.gradleProperty(gradleProperty).orNull
        ?: localProperties.getProperty(localProperty, "")

android {
    namespace = "ai.january.partner.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.january.partner.demo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val apiKey = demoConfiguration("januaryApiKey", "january.apiKey")
        val partnerTokenUrl = demoConfiguration("januaryPartnerTokenUrl", "january.partnerTokenUrl")
        val partnerSessionToken = demoConfiguration("januaryPartnerSessionToken", "january.partnerSessionToken")
        buildConfigField("String", "JANUARY_API_KEY", "\"${apiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "JANUARY_PARTNER_TOKEN_URL", "\"${partnerTokenUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "JANUARY_PARTNER_SESSION_TOKEN", "\"${partnerSessionToken.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel2api35") {
                    device = "Pixel 2"
                    apiLevel = 35
                    systemImageSource = "aosp"
                }
            }
        }
    }

    sourceSets["main"].assets.srcDir("../sdk/src/test/resources")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("ai.january:january-sdk-android:0.1.0")

    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    // UI tests use the local debug SDK to target the fixture server; the demo APK uses the Maven artifact above.
    androidTestImplementation(project(":sdk"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
}
