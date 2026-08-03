plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktfmt)
    id("maven-publish")
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

    publishing { singleVariant("release") { withSourcesJar() } }
}

ktfmt { kotlinLangStyle() }

detekt {
    baseline = file("detekt-baseline.xml")
    buildUponDefaultConfig = true
    ignoreFailures = false
}

// JitPack invokes Gradle with -Pversion=<tag>, which sets project.version directly.
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.machado001"
            artifactId = "in-app-review"
            version = project.version.toString()

            afterEvaluate { from(components["release"]) }
        }
    }
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
