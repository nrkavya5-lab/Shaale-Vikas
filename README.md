# Shaale-Vikas (School Development) 🏫

**Shaale-Vikas** is a transparent, real-time bridge designed to connect rural government schools in India with their global alumni network. The platform empowers school headmasters to publish urgent needs and allows alumni to give back to their alma mater through secure donations and impact tracking.

---

## 🌟 Key Features

### For Schools (Headmasters)
- **Publish Needs:** Post urgent school requirements (e.g., leaking roofs, lab equipment, sanitation) with descriptions, target goals, and photos.
- **Impact Reporting:** Close the loop by sharing "Before & After" photos and reports once a project is completed.
- **Financial Dashboard:** Track total funds received and the number of problems solved in real-time.

### For Alumni
- **Global Discovery:** Search and browse through a live feed of school needs across various regions.
- **Seamless Donations:** Contribute funds directly to specific causes with live progress tracking.
- **Hall of Fame:** A dedicated leaderboard honoring top donors and supporters.
- **Transparency:** View verified impact reports to see exactly how contributions are making a difference.

---

## 🚀 Technical Stack

- **UI/UX:** [Jetpack Compose](https://developer.android.com/compose) (100% Kotlin) for a modern, responsive material design.
- **Backend:** [Firebase Firestore](https://firebase.google.com/products/firestore) for real-time NoSQL data synchronization.
- **Authentication:** [Firebase Auth](https://firebase.google.com/products/auth) (Email/Password & Google Sign-In support).
- **Cloud Storage:** [Firebase Storage](https://firebase.google.com/products/storage) for hosting high-resolution school and impact photos.
- **Image Loading:** [Coil](https://coil-kt.github.io/coil/) for asynchronous, optimized image rendering.
- **Architecture:** MVVM (Model-View-ViewModel) with Kotlin Coroutines and StateFlow.

---

## 📸 Screenshots

| Landing Screen | Needs Feed | Impact Reports |
| :---: | :---: | :---: |
| ![Landing](https://via.placeholder.com/200x400?text=Landing+UI) | ![Needs](https://via.placeholder.com/200x400?text=Needs+Feed) | ![Impact](https://via.placeholder.com/200x400?text=Impact+UI) |

---

## 🛠️ Installation & Setup

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17+
- A Firebase Project

### Setup Steps
1. **Clone the repository:**
   ```bash
   git clone https://github.com/nrkavya5-lab/Shaale-Vikas.git
   ```
2. **Firebase Configuration:**
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with package name `com.example.greetingcard`.
   - Download `google-services.json` and place it in the `app/` directory.
   - Enable **Firestore**, **Authentication**, and **Storage** in the console.
3. **Build & Run:**
   - Open the project in Android Studio.
   - Sync Gradle and run the app on an emulator or physical device.

---

## 🛡️ Security & Roles
The app utilizes a dual-role system:
- **Admin (Headmaster):** Full access to create needs and post impact reports.
- **Alumni:** Access to search, view details, and donate.

---

## 🤝 Contributing
Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📝 License
Distributed under the MIT License. See `LICENSE` for more information.

---

*“Educating the mind without educating the heart is no education at all.” – Aristotle*
