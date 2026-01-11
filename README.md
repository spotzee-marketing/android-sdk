[![](https://jitpack.io/v/spotzee-marketing/android-sdk.svg)](https://jitpack.io/#spotzee-marketing/android-sdk)

<p align="center">
  <picture>
    <source
      media="(prefers-color-scheme: dark)"
      srcset="https://raw.githubusercontent.com/spotzee-marketing/android-sdk/main/.github/assets/logo-dark.svg"
    />
    <source
      media="(prefers-color-scheme: light)"
      srcset="https://raw.githubusercontent.com/spotzee-marketing/android-sdk/main/.github/assets/logo-light.svg"
    />
    <img
      width="400"
      alt="Spotzee Logo"
      src="https://raw.githubusercontent.com/spotzee-marketing/android-sdk/main/.github/assets/logo-light.svg"
    />
  </picture>
</p>

# Spotzee Android SDK

## Installation
Installing the Spotzee Android SDK will provide you with user identification, deeplink unwrapping and basic tracking functionality. The Android SDK is available through JitPack or through manual installation.

### Version Information
- The Spotzee Android SDK supports SDK 21+

### Install the SDK
In your **settings.gradle** add the JitPack repository:
```gradle
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

In your **build.gradle** add:
```gradle
dependencies {
    implementation 'com.github.spotzee-marketing:android-sdk:1.0.5'
}
```

## Usage
### Initialize
Before using any methods, the library must be initialized with an API key.

```kotlin
val analytics = Spotzee.initialize(app, "YOUR_API_KEY")
```

### Identify
You can handle the user identity of your users by using the `identify` method. This method works in combination either/or associate a given user to your internal user ID (`external_id`) or to associate attributes (traits) to the user. By default all events and traits are associated with an anonymous ID until a user is identified with an `external_id`. From that point moving forward, all updates to the user and events will be associated to your provider identifier.
```kotlin
analytics.identify(
    id = USER_ID,
    traits = mapOf(
        "first_name" to "John",
        "last_name" to "Doe"
    )
)
```

### Events
If you want to trigger journey and list updates off of things a user does within your app, you can pass up those events by using the `track` method.
```kotlin
analytics.track(
    event = "Application Opened",
    properties = mapOf("property" to true)
)
```

You can also update user profile fields inline with the event:
```kotlin
analytics.track(
    event = "Tapped Button",
    properties = emptyMap(),
    user = TrackUser(
        timezone = "America/New_York",
        locale = "en-US"
    )
)
```

### Register Device
In order to send push notifications to a given device you need to register for notifications and then register the device with Spotzee. You can do so by using the `register` method. If a user does not grant access to send notifications, you can also call this method without a token to register device characteristics.
```kotlin
analytics.register(
    token = token,
    appBuild = BuildConfig.VERSION_CODE,
    appVersion = BuildConfig.VERSION_NAME
)
```

### Deeplink Navigation
To allow for click tracking links in emails can be click-wrapped in a Spotzee url that then needs to be unwrapped for navigation purposes. For information on setting this up on your platform, please see our deeplink documentation.

Spotzee includes a method which checks to see if a given URL is a Spotzee URL and if so, unwraps the url, triggers the unwrapped URL and calls the Spotzee API to register that the URL was executed.

To start using deeplinking in your app, add your Spotzee deployment URL in your activity `intent-filter`. Example in the sample project [here](samples/kotlin-android-app/src/main/AndroidManifest.xml).

Next, you'll need to update your apps code to support unwrapping the Spotzee URLs that open your app. To do so, use the `getUriRedirect(universalLink)` method. In your app delegate's `onNewIntent(intent)` method, unwrap the URL and pass it to the handler:

```kotlin
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)

    val uri = intent?.data
    if (uri != null) {
        val redirect = analytics.getUriRedirect(uri)
    }
}
```

Spotzee links will now be automatically read and opened in your application.

## Example

Explore our [example project](samples/kotlin-android-app) which includes basic usage.

## License

Copyright (c) 2025 Spotzee. All rights reserved.
