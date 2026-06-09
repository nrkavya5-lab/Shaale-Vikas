# Shaale-Vikas (School Development) Documentation

> A transparent, real-time bridge connecting rural government schools in India with their global alumni network.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Features](#features)
  - [For Schools (Headmasters)](#for-schools-headmasters)
  - [For Alumni](#for-alumni)
- [Installation Guide](#installation-guide)
  - [Prerequisites](#prerequisites)
  - [Setup Steps](#setup-steps)
- [Firebase Configuration Guide](#firebase-configuration-guide)
  - [Creating a Firebase Project](#creating-a-firebase-project)
  - [Adding the Android App](#adding-the-android-app)
  - [Enabling Services](#enabling-services)
  - [Firestore Security Rules](#firestore-security-rules)
  - [Storage Security Rules](#storage-security-rules)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
  - [MVVM with Kotlin Coroutines](#mvvm-with-kotlin-coroutines)
  - [Data Flow Diagram](#data-flow-diagram)
- [User Roles and Access Control](#user-roles-and-access-control)
- [Security and Roles](#security-and-roles)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

Shaale-Vikas (translated as "School Development") is an Android application built entirely in Kotlin using Jetpack Compose. It enables school headmasters in rural government schools to publish urgent infrastructure needs, track fundraising progress, and share impact reports. Alumni across the globe can discover these needs, donate directly to specific causes, view a Hall of Fame leaderboard of top supporters, and see verified before/after impact reports.

The application uses Firebase as its complete backend-as-a-service, providing real-time data synchronization via Firestore, authentication via Firebase Auth, and image storage via Firebase Storage.

---

## Tech Stack

| Component                       | Technology                                                   |
| ------------------------------- | ------------------------------------------------------------ |
| UI Framework                    | Jetpack Compose (Material 3)                                 |
| Language                        | Kotlin 2.0.21                                                |
| Architecture                    | MVVM (Model-View-ViewModel)                                  |
| Asynchronous Programming        | Kotlin Coroutines with StateFlow                             |
| Backend/Database                | Firebase Firestore (real-time NoSQL)                         |
| Authentication                  | Firebase Auth (Email/Password + Google Sign-In)              |
| Image Storage                   | Firebase Storage                                             |
| Image Loading                   | Coil (Compose integration)                                   |
| Build System                    | Gradle with Kotlin DSL + Version Catalog                     |
| Minimum SDK                     | API 24 (Android 7.0)                                         |
| Target SDK                      | API 35 (Android 15)                                          |
| Android Gradle Plugin           | 9.0.1                                                        |

---

## Features

### For Schools (Headmasters)

- **Publish Urgent Needs:** Post school requirements (e.g., leaking roofs, laboratory equipment, sanitation facilities) with a title, detailed description, target fundraising goal, and optional photo. Needs are tagged as urgent to attract immediate attention.
- **Impact Reporting:** Close the loop on completed projects by publishing before-and-after photo reports. Alumni can see exactly how their contributions were used.
- **Financial Dashboard:** View a real-time dashboard showing total funds received and the count of problems solved, along with recent donation activity.

### For Alumni

- **Global Discovery:** Browse a live feed of school needs from across regions. Search and filter by school name or problem title to find causes matching your interests.
- **Seamless Donations:** Contribute funds directly to a specific need with live progress tracking. Each need displays a percentage-funded indicator and a real-time progress bar.
- **Hall of Fame:** A dedicated leaderboard honoring top supporters by donation amount, showcasing the community's collective impact.
- **Transparency:** View verified impact reports with before/after photos to see exactly how contributions are making a difference in real schools.

---

## Installation Guide

### Prerequisites

- Android Studio Ladybug or newer (2024.1+)
- JDK 17 or later
- A Google account for Firebase Console access
- An Android device or emulator running API 24+

### Setup Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/nrkavya5-lab/Shaale-Vikas.git
   ```

2. **Open in Android Studio:**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory and click OK
   - Allow Gradle to sync and download dependencies

3. **Configure Firebase:**
   - Follow the [Firebase Configuration Guide](#firebase-configuration-guide) below to create a Firebase project, download `google-services.json`, and place it in `app/`.

4. **Build and Run:**
   - Select a device or emulator from the toolbar
   - Click the Run button (green triangle) or use `Shift+F10`
   - The app will install and launch automatically

5. **Troubleshooting:**
   - If Gradle sync fails, verify JDK 17+ is configured in `File > Project Structure > SDK Location`
   - If Firebase features fail, confirm `google-services.json` is present in `app/` directory
   - If build errors persist, run `File > Invalidate Caches and Restart`

---

## Firebase Configuration Guide

### Creating a Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" (or "Create a project")
3. Enter a project name (e.g., "Shaale-Vikas")
4. Disable Google Analytics if not needed, or configure it as desired
5. Click "Create project" and wait for provisioning

### Adding the Android App

1. In the Firebase Console project overview, click the Android icon to add an Android app
2. Enter the package name: `com.example.greetingcard`
3. Optionally enter an app nickname (e.g., "Shaale-Vikas Android")
4. Enter the debug signing certificate SHA-1 (optional, needed for Google Sign-In)
5. Click "Register app"
6. Download the `google-services.json` file
7. Place the downloaded file in `app/google-services.json` (overwrite the existing placeholder if present)

### Enabling Services

#### Firestore Database

1. In the Firebase Console, navigate to "Firestore Database"
2. Click "Create database"
3. Choose "Start in test mode" (see security rules below for production)
4. Select a location closest to your user base
5. Click "Enable"

#### Firebase Authentication

1. Navigate to "Authentication" > "Sign-in method"
2. Enable "Email/Password" provider
3. (Optional) Enable "Google" provider and configure the OAuth consent screen with your SHA-1 fingerprint
4. Click "Save"

#### Firebase Storage

1. Navigate to "Storage"
2. Click "Get started"
3. Choose "Start in test mode" (see security rules below)
4. Select a location and click "Enable"

### Firestore Security Rules

For development, you can use test mode. For production, deploy the following rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Needs collection: anyone can read, only authenticated admins can write
    match /needs/{needId} {
      allow read: if true;
      allow create, update: if request.auth != null
        && request.auth.uid != null
        && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'Admin';
      allow delete: if false; // Soft delete only (is_active field)
    }

    // Impact collection: anyone can read, only admins can write
    match /impact/{impactId} {
      allow read: if true;
      allow create: if request.auth != null
        && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'Admin';
    }

    // Users collection: users can read/write their own data
    match /users/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow create: if request.auth != null && request.auth.uid == userId;
      allow update: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### Storage Security Rules

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /needs/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null
        && firestore.get(/databases/(default)/documents/users/$(request.auth.uid)).data.role == 'Admin';
    }
    match /impact/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null
        && firestore.get(/databases/(default)/documents/users/$(request.auth.uid)).data.role == 'Admin';
    }
  }
}
```

---

## Project Structure

```
Shaale-Vikas/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/greetingcard/
│   │   │   │   ├── MainActivity.kt          # Entry point, navigation, all screens, composables
│   │   │   │   ├── model/
│   │   │   │   │   └── FirebaseModels.kt     # Data models (Need, ImpactItem, Donor, UserProfile)
│   │   │   │   ├── data/
│   │   │   │   │   └── FirebaseManager.kt    # Firebase Auth, Firestore, Storage abstraction layer
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── MainViewModel.kt      # ViewModel with StateFlow for reactive UI
│   │   │   │   └── ui/theme/
│   │   │   │       ├── Color.kt              # Theme color definitions
│   │   │   │       ├── Theme.kt              # Material 3 light/dark theme configuration
│   │   │   │       └── Type.kt               # Typography definitions
│   │   │   ├── res/
│   │   │   │   ├── drawable/                 # Vector drawables and image assets
│   │   │   │   ├── mipmap-*/                 # Launcher icons (various densities)
│   │   │   │   ├── values/                   # Strings, colors, themes XML resources
│   │   │   │   └── xml/                      # Backup rules, data extraction rules
│   │   │   ├── AndroidManifest.xml           # App manifest
│   │   │   └── google-services.json          # Firebase configuration (per-project)
│   │   ├── test/                             # Unit tests (JUnit 4)
│   │   │   └── .../ExampleUnitTest.kt
│   │   └── androidTest/                      # Instrumented tests (Compose UI tests)
│   │       └── .../ExampleInstrumentedTest.kt
│   ├── build.gradle.kts                      # App-level build script
│   ├── proguard-rules.pro                    # ProGuard rules for release builds
│   └── .gitignore
├── gradle/
│   ├── wrapper/                              # Gradle wrapper binaries
│   ├── libs.versions.toml                    # Version catalog (dependency management)
│   └── gradle-daemon-jvm.properties          # Daemon JVM configuration
├── build.gradle.kts                          # Project-level build script
├── settings.gradle.kts                       # Project settings (name: "greeting card")
├── gradle.properties                         # Gradle JVM and AndroidX settings
├── gradlew / gradlew.bat                     # Gradle wrapper scripts
├── .gitignore
├── .idea/                                    # Android Studio IDE settings
└── README.md
```

---

## Architecture

### MVVM with Kotlin Coroutines

The application follows the Model-View-ViewModel (MVVM) architecture pattern, ensuring a clean separation of concerns and testability.

**Layers:**

1. **Model** (`model/FirebaseModels.kt`)
   - Data classes: `Need`, `ImpactItem`, `Donor`, `UserProfile`
   - Annotated with `@IgnoreExtraProperties` for Firestore deserialization
   - Each model maps directly to a Firestore collection

2. **Data Layer** (`data/FirebaseManager.kt`)
   - Singleton-style class wrapping all Firebase interactions
   - Firebase Auth: `getCurrentUserId()`, `logout()`
   - Firestore: real-time `Flow<List<Need>>` via `callbackFlow` and snapshot listeners; suspend functions for writes (`addNeed`, `addImpactItem`, `updateNeedDonation`)
   - Firebase Storage: `uploadImage()` for need photos and before/after impact photos
   - Uses `kotlinx.coroutines.tasks.await()` to bridge Firebase Tasks into coroutines

3. **ViewModel Layer** (`viewmodel/MainViewModel.kt`)
   - Extends `ViewModel` and uses `viewModelScope` for coroutine management
   - Exposes `StateFlow` properties: `userProfile`, `needs`, `impactItems`, `isLoading`
   - Functions: `addNeed()`, `addImpact()`, `donate()`, `logout()`
   - On init, collects real-time Firestore flows and loads the current user's profile

4. **View Layer** (`MainActivity.kt`)
   - Single-activity architecture with Compose navigation via an enum-based screen state machine (`Screen` enum: `Landing`, `LoginHm`, `LoginAlumni`, `Dash`, `Needs`, `NeedDetail`, `Impact`, `Donors`, `Settings`)
   - All screens are composable functions defined within `MainActivity.kt`
   - `ShaaleVikasApp()` is the root composable, holding global state and providing the scaffold with bottom navigation
   - FAB and bottom sheet modals for creating needs, impact reports, and donations
   - Image loading via Coil's `AsyncImage` composable
   - Animated screen transitions using `AnimatedContent`

### Data Flow

```
User Action (UI Event)
    |
    v
Composable (View) --- calls ---> MainViewModel (ViewModel)
    |                                      |
    |                                      v
    |                            FirebaseManager (Data Layer)
    |                                      |
    |                                      v
    |                            Firebase Auth / Firestore / Storage
    |                                      |
    |                                      v
    |                            Snapshot Listener / Task Result
    |                                      |
    v                                      v
StateFlow updates ---> Composable recomposes with new state
```

---

## User Roles and Access Control

The app implements a dual-role system defined in `UserProfile.role`:

| Role       | Capabilities                                                                 |
| ---------- | ---------------------------------------------------------------------------- |
| Admin      | Publish school needs with photos; post impact reports with before/after photos; view financial dashboard |
| Alumni     | Browse and search global needs; donate to specific causes; view impact reports; access Hall of Fame leaderboard |
| None       | Can only see the landing screen; must register/login to proceed              |

Role assignment happens at registration:
- **Headmasters** register with their official UDISE (Unified District Information System for Education) code
- **Alumni** register with their passing year

The current role is tracked in-memory via a `MutableState<Role>` in `ShaaleVikasApp()` and stored in Firestore as `UserProfile.role`. Navigation and UI elements (FAB, bottom bar items, buttons) are conditionally rendered based on the current role.

---

## Security and Roles

Firebase handles the backend security layer:

- **Authentication:** Firebase Auth manages user identity. Email/password authentication is the primary method, with Google Sign-In available as an additional provider.
- **Firestore Security Rules:** Enforce role-based access at the database level. Needs and impact reports are readable by anyone but writable only by users with the `Admin` role. User profiles are restricted to the owning user.
- **Storage Security Rules:** Image uploads are restricted to authenticated admin users. All images are publicly readable to enable sharing and display.
- **Password Security:** Passwords are stored securely by Firebase Auth using bcrypt hashing. The app never stores credentials locally.
- **Session Management:** Logout clears both the in-memory state and Firebase Auth session.

---

## Contributing

Contributions are welcome and appreciated. To contribute:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/AmazingFeature`
3. Commit your changes: `git commit -m 'Add some AmazingFeature'`
4. Push to the branch: `git push origin feature/AmazingFeature`
5. Open a Pull Request

Please ensure your code follows the existing style conventions and includes appropriate tests.

---

## License

Distributed under the MIT License. See `LICENSE` for more information.

---

*"Educating the mind without educating the heart is no education at all." -- Aristotle*
