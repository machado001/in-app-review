# in-app-review

A small Android library wrapping the Google Play [In-App Review API](https://developer.android.com/guide/playcore/in-app-review).
It never asks Play for a review flow — not even the request itself — unless a
`ReviewPromptPolicy` you supply says the user is due. Play applies its own server-side
quota, but that quota is invisible to your app and won't stop you from calling
`requestReviewFlow()` on every launch; this library puts that decision under your control
instead.

## Install

Add JitPack as a repository (in `settings.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency, pinned to a release tag:

```kotlin
dependencies {
    implementation("com.github.machado001:in-app-review:1.0.0")
}
```

## Usage

Implement `ReviewPromptPolicy` with your own usage-gating logic — how many sessions, how much
time, whatever signal makes sense for your app:

```kotlin
class MyReviewPromptPolicy(/* ... */) : ReviewPromptPolicy {
    override suspend fun shouldAskForReview(): Boolean { /* your gating logic */ }
    override suspend fun onReviewAsked() { /* record that an ask happened */ }
}
```

Then, from an `Activity`:

```kotlin
val reviewManager = GooglePlayReviewManager(
    reviewManager = ReviewManagerFactory.create(activity),
    promptPolicy = MyReviewPromptPolicy(/* ... */)
)

lifecycleScope.launch {
    reviewManager.maybeLaunchReview(activity)
}
```

`maybeLaunchReview` is the only entry point. It consults the policy first; if the policy says
no, nothing is requested from Play at all. If it says yes, the ask is recorded via
`onReviewAsked()` *before* the flow launches — a failed or throttled flow still cost the user's
goodwill for this session, so it counts against your policy's cap regardless of outcome.

## Building locally

```
./gradlew build
./gradlew test
```

Robolectric is pinned to Android SDK 36 (`src/test/resources/robolectric.properties`) since
Robolectric doesn't yet support `compileSdk 37`. Running the test suite needs **JDK 21** —
Robolectric's SDK 36 sandbox refuses to build under JDK 17, even though the library itself
compiles against Java 17 (`compileOptions`).
