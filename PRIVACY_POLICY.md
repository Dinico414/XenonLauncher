# Privacy Policy

**Last Updated:** September 23, 2026

This privacy policy applies to the **Xenon Launcher** application for Android devices. We believe in absolute transparency and value your privacy above everything else.

> ### 🛡️ Core Privacy Principles
> - **100% Local Data:** All data generated, parsed, or processed by Xenon Launcher remains strictly on your local device.
> - **No External Transmission:** We do not operate any external tracking, analytics, or cloud synchronization servers. No personal data, telemetry, or usage metrics are collected or transmitted to anyone.
> - **No Data Harvesting:** We do not track what apps you open, your layout preferences, your location, or any other activity.

---

## 1. Application Functionality & Permission Usage

To function effectively as a home screen replacement, Xenon Launcher requires certain Android system permissions. Below is an exhaustive list of requested permissions and why they are required:

### 📦 Applications & System Management
* **`android.permission.QUERY_ALL_PACKAGES`**  
  *Purpose:* Required to query and retrieve the list of installed applications on your device, allowing them to be displayed in the launcher home screens, app drawer, and search index.
* **`android.permission.REQUEST_DELETE_PACKAGES`**  
  *Purpose:* Allows you to uninstall third-party applications directly via the launcher's user interface shortcuts.
* **`android.permission.EXPAND_STATUS_BAR`**  
  *Purpose:* Used to trigger expanding the status bar notifications tray programmatically when a swipe-down gesture is performed on the home screen.

### 🔔 Services & Interactions
* **`android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`**  
  *Purpose:* Used to detect incoming notifications solely for displaying custom notification badges or counts directly on top of app icons or inside custom widgets.
* **`android.permission.BIND_ACCESSIBILITY_SERVICE`**  
  *Purpose:* Optional feature enabling convenient user-triggered shortcuts or navigation gestures (such as double-tapping the screen to turn off the display or swiping down to expand the status bar panel).

### 🎨 Personalization & Files
* **`android.permission.MANAGE_EXTERNAL_STORAGE` / `android.permission.READ_EXTERNAL_STORAGE` / `android.permission.READ_MEDIA_AUDIO`**  
  *Purpose:* Used to load custom icon packs, local wallpapers, or audio assets chosen explicitly by you to personalize the user interface.
* **`android.permission.SET_WALLPAPER`**  
  *Purpose:* Allows the launcher to set your system or lock screen wallpaper from within the launcher application settings or themes menu.

### 📍 Local Context & Tools
* **`android.permission.READ_CONTACTS`**  
  *Purpose:* Allows the search feature or optional contact shortcuts to find contact information directly on your device for rapid interaction.
* **`android.permission.ACCESS_FINE_LOCATION` / `android.permission.ACCESS_COARSE_LOCATION`**  
  *Purpose:* Used locally to deliver location-based features such as local weather widgets or astronomical clock visualizations.
* **`android.permission.READ_CALENDAR` / `android.permission.WRITE_CALENDAR`**  
  *Purpose:* Enables optional calendar/agenda widgets to fetch and show upcoming schedule events directly on your home screen.

### 🎵 Audio & Visualizer
* **`android.permission.RECORD_AUDIO` / `android.permission.MODIFY_AUDIO_SETTINGS`**  
  *Purpose:* Required by the Android OS for the real-time audio spectrum analyzer and music visualizer on the media player page and dock. Although classified under audio recording permissions by Android system security, **no microphone voice audio is recorded, saved to files, or transmitted**. Audio frequency data is analyzed strictly in temporary memory in real time solely to render visualizer wave/bar graphics.

### ⚙️ Utilities & Network
* **`android.permission.POST_NOTIFICATIONS`**  
  *Purpose:* Allows the launcher to issue system notifications when critical or user-requested operations are running.
* **`android.permission.VIBRATE`**  
  *Purpose:* Provides haptic/vibrational feedback when long-pressing, rearranging icons, or invoking gestures.
* **`android.permission.INTERNET`**  
  *Purpose:* Included for standard network capabilities such as fetching weather updates from public data feeds and optional user-initiated Google Authentication for Cloud Backup & Restore. No app usage telemetry, device statistics, or layout tracking data is ever sent across the network.

---

## 2. Optional Cloud Backup & Authentication

Xenon Launcher provides an **optional** Cloud Backup & Restore feature:
- If you choose to sign in with your Google account via Firebase Authentication, your identity is verified using standard Google ID tokens.
- Cloud Backup strictly stores your user-configured launcher layout, preferences, and icon settings under your account so you can restore them on other devices.
- Account sign-in is entirely optional. If you do not sign in, no data leaves your local device.

---

## 3. Data Security & Storage

Because all primary launcher data stays local to the sandbox allocated to the application on your device, your data's security relies on the built-in security features of the Android operating system. Xenon Launcher does not employ any third-party analytics SDKs (such as Firebase Analytics, Flurry, or Mixpanel) or advertisement trackers.

---

## 4. Changes to This Privacy Policy

We may update our Privacy Policy from time to time as new features are added. Any updates to this policy will be posted within the application repository and official release documentation. You are advised to review this page periodically for any changes.

---

## 5. Contact Us

If you have any questions, suggestions, or bug reports regarding this Privacy Policy or the security of the application, please feel free to reach out:

- **Developer Name:** Lead: Nico (Dinico414) / Company: Xenonware
- **Contact Email:** [dinicokustom@gmail.com](mailto:dinicokustom@gmail.com)

---
© 2026 Xenon Launcher. All Rights Reserved.
