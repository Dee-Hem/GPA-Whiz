# GPA Whiz

**GPA Whiz** is an offline academic management application built to help university students calculate and track their GPA/CGPA, plan their academic performance, manage schedules, and keep track of scholarship opportunities and applications.

It is designed to bring several everyday academic tools into one place, while keeping students' data accessible offline.

## ✨ Features

### 🎓 GPA & CGPA Calculator

* Calculate semester GPA using courses, grades, and credit units.
* Track cumulative academic performance across semesters.
* Keep an organized record of academic results.

### 📈 CGPA What-If Simulator

* Explore different grade scenarios and their potential impact on CGPA.
* Set academic targets and see what may be required to achieve them.
* Experiment with possible future results before making academic decisions.

### 📚 Academic Record

* Store courses, grades, credit units, and semester results.
* Maintain an offline history of academic performance.
* Review previous academic results in one place.

### 🗓️ Timetable

* Organize class schedules.
* Keep important academic activities accessible within the application.

### ⏰ Alarm & Calendar Integration

* Set reminders for academic activities.
* Connect schedules and reminders with the device's native alarm and calendar features.

### 🎓 Scholarship Application Tracker

Keep scholarship opportunities and applications organized alongside your academic records.

* Add and track scholarship opportunities.
* Record application deadlines.
* Monitor application status.
* Keep important scholarship information organized.
* Integrate scholarship deadlines with the local calendar.
* Export scholarship application records for personal documentation.

### 📄 PDF Export

* Export academic records and other relevant information to PDF.
* Create organized copies of your records for personal use and documentation.

### 📊 Excel Export

* Export scholarship application and related records to Excel.
* Make your data easier to review, manage, or archive.

### 💾 Backup & Restore

* Back up your academic and application data.
* Restore previously saved records when needed.

### 📑 Offline Transcript

* Keep an organized record of academic performance.
* Access your academic history without requiring an internet connection.

### 🔒 Offline-First

GPA Whiz is designed around an **offline-first experience**.

Your academic and scholarship information is stored locally, allowing you to continue using the core features without depending on an internet connection.

## 🎯 Why GPA Whiz?

University students often have to manage several different aspects of their academic life at once—from calculating GPA and planning their CGPA to remembering class schedules and keeping track of scholarship deadlines.

GPA Whiz brings these tasks together in one application.

The goal is not simply to tell students **what their GPA is**, but to help them understand their academic progress, plan ahead, stay organized, and make better decisions about their academic journey.

## 🛠️ Tech Stack

### Core Platform & Language

- **Platform**: Android (Min SDK 24, Target SDK 36)
- **Language**: **Kotlin** (100% type-safe, concise, and modern)
- **Build System**: **Gradle (Kotlin DSL)** with Version Catalogs for centralized dependency management.

### UI & Design

- **UI Framework**: **Jetpack Compose** (Declarative UI)
- **Design System**: **Material Design 3 (M3)** with Dynamic Color support.
- **Iconography**: Material Symbols (Extended) for a comprehensive visual language.
- **Theming**: Centralized custom theme with support for accessibility and various screen sizes.

### Architecture & State Management

- **Pattern**: **MVVM (Model-View-ViewModel)** for clean separation of concerns.
- **Components**:
  - **ViewModel**: Manages UI-related data and lifecycle awareness.
  - **Coroutines & Flow**: Handles asynchronous operations and reactive data streams.
  - **StateFlow**: Used for state management within Compose.

### Data & Persistence

- **Local Database**: **Room (SQLite)** for robust, offline-first data storage.
- **Serialization**: **Moshi** for high-performance JSON processing.
- **Asset Management**: Embedded PDF and XLSX generators for reporting.

### Networking (Infrastructure Ready)

- **Client**: **Retrofit** + **OkHttp** for REST API communication.
- **Interceptors**: Logging interceptors for network debugging and telemetry.

### Testing & Quality

- **Unit Testing**: JUnit 4 and **Robolectric** for fast, local JVM tests.
- **Visual Testing**: **Roborazzi** for automated screenshot and UI regression testing.
- **Code Analysis**: **KSP (Kotlin Symbol Processing)** for efficient compile-time code generation.

### System Integrations

- **Native Intents**: Integration with the Android System Calendar and Alarm Clock apps.
- **Permissions**: Dynamic runtime permission handling for system alerts and notifications.

## 🚀 Getting Started

### Prerequisites

* Android Studio
* Android SDK
* A compatible Android device or emulator

### Installation

Clone the repository:

```bash
git clone https://github.com/YOUR-USERNAME/gpa-whiz.git
```

Navigate into the project:

```bash
cd gpa-whiz
```

Open the project in **Android Studio** and allow Gradle to sync the dependencies.

Build and run the application on a compatible Android device or emulator.

## 📸 Screenshots

<img width="720" height="1600" alt="Screenshot_20260828_105524" src="https://github.com/user-attachments/assets/faab3d6a-45af-4f8f-8844-757d5fc73a81" />
<img width="720" height="1600" alt="Screenshot_20260828_105514" src="https://github.com/user-attachments/assets/71189274-f063-47ab-9d2f-af8da099cf0e" />


Suggested screenshots:

* GPA Dashboard
* GPA Calculator
* CGPA What-If Simulator
* Academic Records
* Timetable
* Scholarship Application Tracker
* Scholarship details/deadline view
* Export/backup features

## 🗺️ Roadmap

Potential improvements include:

* [ ] More academic performance analytics
* [ ] Enhanced CGPA planning tools
* [ ] More scholarship tracking options
* [ ] Improved deadline and reminder management
* [ ] Additional export formats
* [ ] Enhanced backup and restore functionality
* [ ] More academic insights and recommendations
* [ ] Additional customization options

## 🤝 Contributing

Contributions, suggestions, and bug reports are welcome.

To contribute:

1. Fork the repository.
2. Create a new branch.
3. Make your changes.
4. Commit your changes.
5. Open a pull request.

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.

## 👤 Author

**Olumide Adeleke**

Computer Science student and software developer interested in building practical technology for education and social impact.

---

⭐ If you find GPA Whiz useful, consider starring the repository.
