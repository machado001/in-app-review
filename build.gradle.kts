plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktfmt)
}

android {
    namespace = "com.machado001.google.inappreview"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

ktfmt { kotlinLangStyle() }

detekt {
    config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
    baseline = file("detekt-baseline.xml")
    buildUponDefaultConfig = true
    ignoreFailures = false
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget = libs.versions.javaVersion.get()
}

dependencies {
    implementation(libs.androidx.core.ktx)

    api(libs.review)
    api(libs.review.ktx)

    implementation(libs.logcat)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
