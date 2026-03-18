# 5/3/1 Workout App for Android

A free, open-source Android app for tracking Jim Wendler's 5/3/1 strength training program. Built with Jetpack Compose and Material Design 3.

[![Download APK](https://img.shields.io/badge/Download-APK-green?style=for-the-badge&logo=android)](app-debug.apk)

---

## Screenshots

<p align="center">
  <img src="screenshots/01_setup.png" width="180" alt="Setup Screen">
  <img src="screenshots/02_cycle_overview.png" width="180" alt="Cycle Overview">
  <img src="screenshots/03_cycle_progress.png" width="180" alt="Cycle with Completion Status">
  <img src="screenshots/04_next_cycle.png" width="180" alt="Start Next Cycle Dialog">
</p>

<p align="center">
  <img src="screenshots/05_workout.png" width="180" alt="Workout Screen with Rest Timer">
  <img src="screenshots/06_secondary_exercises.png" width="180" alt="Secondary Exercises">
  <img src="screenshots/07_add_exercise.png" width="180" alt="Add Secondary Exercise Dialog">
</p>

| Screen | Description |
|--------|-------------|
| **Setup** | Enter your 1RM or training maxes for OHP, Squat, Bench, and Deadlift |
| **Cycle Overview** | See all 4 weeks at a glance with training maxes and completion status |
| **Completion Tracking** | Green = all sets done, Orange = main sets done (warmups skipped) |
| **Cycle Progression** | One-tap progression with automatic TM increases (+5/+10 lbs) |
| **Workout** | Execute sets with rest timer, plate calculator, and AMRAP tracking |
| **Secondary Exercises** | Add accessories like dips, rows, curls - track sets per workout |
| **Add Exercise** | Specify name, sets, reps, and weight for any accessory movement |

---

## Features

### Core 5/3/1 Program
- Full implementation of Wendler's 5/3/1 protocol with all four weeks (5/5/5, 3/3/3, 5/3/1, Deload)
- Tracks four main lifts: **Overhead Press**, **Squat**, **Bench Press**, **Deadlift**
- Automatic warmup set generation at 40%, 50%, and 60% of training max
- AMRAP (As Many Reps As Possible) tracking on final working sets
- Automatic training max calculation at 90% of your 1RM

### Rest Timer
- Configurable rest timer (60s, 90s, 120s, 180s, or custom 10-600s)
- Visual countdown with progress bar
- Audio chime when rest period ends
- One-tap skip button

### Plate Calculator
- Shows exact plates needed per side for every set
- Supports standard plate sizes (45, 35, 25, 10, 5, 2.5 lbs)
- Configurable bar weight
- "Bar only" display for light warmups

### Secondary Exercises
- Add custom accessory exercises to any main lift
- Track sets, reps, and weight for each
- Progress persists within a cycle
- Templates carry over when starting a new cycle

### Cycle Progression
- One-tap cycle completion with confirmation
- Automatic training max increases:
  - Upper body (OHP, Bench): **+5 lbs / +2.5 kg**
  - Lower body (Squat, Deadlift): **+10 lbs / +5 kg**
- Cycle history tracking

### Workout Completion Tracking
- Visual status indicators on the overview grid:
  - **Green** - All sets completed (including warmups)
  - **Orange** - Main sets done, some warmups skipped
  - **Gray** - Not yet started

### Additional Tools
- **1RM Calculator** using the Epley formula
- **Unit support** for both lbs and kg with automatic rounding
- **Dark theme** with Material Design 3 (OLED-friendly)

---

## How It Works

The app follows the standard Wendler 5/3/1 program structure:

| Week | Set 1 | Set 2 | Set 3 (AMRAP) |
|------|-------|-------|----------------|
| **Week 1** (5/5/5) | 65% x 5 | 75% x 5 | 85% x 5+ |
| **Week 2** (3/3/3) | 70% x 3 | 80% x 3 | 90% x 3+ |
| **Week 3** (5/3/1) | 75% x 5 | 85% x 3 | 95% x 1+ |
| **Deload** | 40% x 5 | 50% x 5 | 60% x 5 |

Each workout also includes 3 warmup sets at 40%, 50%, and 60% of your training max.

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| UI Framework | Jetpack Compose |
| Design System | Material Design 3 |
| Architecture | MVVM (ViewModel + State) |
| Navigation | Navigation Compose |
| Persistence | SharedPreferences + GSON |
| Language | Kotlin |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## Installation

### Download APK
1. Download the latest [app-debug.apk](app-debug.apk) from this repo
2. Enable "Install from unknown sources" on your Android device
3. Open the APK and install

### Build from Source
```bash
git clone https://github.com/SashaDmytryshyn1/5-3-1-workout-app-android.git
cd 5-3-1-workout-app-android
./gradlew assembleDebug
# APK output: app/build/outputs/apk/debug/app-debug.apk
```

---

## License

Free and open source. Use it however you want.
