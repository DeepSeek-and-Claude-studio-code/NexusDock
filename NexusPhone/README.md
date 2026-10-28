# Nexus Phone

Android dialer and contacts app built with Kotlin, Jetpack Compose and Material 3.

## Current alpha
- Contact list with name/number search
- Recent call log
- Dial pad and call launch through Android Telecom
- Favorites saved locally
- Request to become the default dialer
- Basic in-call UI wired to an Android `InCallService`

## Build
The GitHub Actions workflow **Build Nexus Phone APK** builds a debug APK for Android API 29+.
The APK is uploaded as the workflow artifact `nexus-phone-debug-apk`.

This is an alpha build. Real-device testing is still required, especially default-dialer role eligibility, incoming-call behavior, and OEM-specific call handling.
