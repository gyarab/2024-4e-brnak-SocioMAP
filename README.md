# 🎨 SocialMap - Android App

SocialMap is a dynamic Android application that allows users to explore and filter various events on an interactive map. With a user-friendly UI, Firebase authentication, and customizable event filters, SocialMap enhances the way users interact with location-based data.

---

## 🚀 Features
- 📍 **Interactive Map** – View events on a real-time Google Map.
- 🔎 **Filter Events** – Filter events by category (e.g., Sports, Music, Festivals).
- 🧑‍💻 **User Authentication** – Login & register using Firebase.
- 🎨 **Custom UI Animations** – Smooth animations for login, register, and UI elements.
- 🛠 **Google Sign-In Integration** – Easy authentication with Google.
- 🔄 **Spinner UI Enhancements** – Custom dropdown menu for event filtering.

---

## 🛠 **Tech Stack**
- **Language**: Java
- **Framework**: Android SDK
- **Authentication**: Firebase Authentication
- **Database**: Firebase Firestore
- **UI Components**: Material Design, Custom Animations
- **Maps**: Google Maps API

---

## 🔧 **Setup & Installation**
### 1️⃣ Clone the Repository
```sh
git clone https://github.com/your-username/your-repository.git
cd your-repository
```

### 2️⃣ Open in Android Studio
1. Open **Android Studio**.
2. Click **"Open"** and select the cloned repository folder.
3. Let **Gradle sync** the project.

### 3️⃣ Set Up Firebase
1. Create a Firebase project on [Firebase Console](https://console.firebase.google.com/).
2. Download **google-services.json** and place it in:
```sh
app/src/main
```

3. Enable **Authentication** → Email & Google Sign-In.
4. Enable **Firestore Database** for storing user events.

### 4️⃣ Get Google Maps API Key
1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Enable **Maps SDK for Android**.
3. Add your API key to:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY_HERE"/>
```
### 5️⃣ Run the App
Click **▶ Run** in Android Studio or use:
```sh
./gradlew assembleDebug
```


