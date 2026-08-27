import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

android {
    namespace = "ai.january.partner.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.january.partner.demo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val apiKey = localProperties.getProperty("january.apiKey", "")
        val partnerTokenUrl = localProperties.getProperty("january.partnerTokenUrl", "")
        val internalApiBaseUrl = localProperties.getProperty("january.internalApiBaseUrl", "")
        buildConfigField("String", "JANUARY_API_KEY", "\"${apiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "JANUARY_PARTNER_TOKEN_URL", "\"${partnerTokenUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "JANUARY_INTERNAL_API_BASE_URL", "\"${internalApiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    sourceSets["main"].assets.srcDir("../sdk/src/test/resources")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":sdk"))

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
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
