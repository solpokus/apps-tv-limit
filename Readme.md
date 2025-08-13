# 📺 TV Limit – Android TV App Usage Limiter

**TV Limit** is a native Android TV application (Kotlin + Jetpack Compose for TV)  
that lets you **block** or **limit usage time** for other apps —  
for example, completely prevent **YouTube TV** from opening, or allow it for only 30 minutes per day.

---

## ✨ Features

- **App Blocking**  
  Choose any installed app (e.g., YouTube TV) and block it.  
  When blocked:
    - **Device Owner mode** → app is *suspended* at system level, cannot open at all.
    - **Soft Block mode** → Accessibility Service detects launches and bounces back to TV Limit.

- **Daily Usage Quotas**  
  Assign a daily limit (in minutes) for any app.  
  When the limit is reached:
    - TV Limit blocks the app until the next day.
    - Limits reset automatically at midnight.

- **PIN-Protected Settings**  
  Lock access to the settings tab so only parents/admins can change restrictions.

- **App Launcher UI**  
  Browse and launch allowed apps directly from TV Limit’s home screen.  
  Blocked apps are hidden.

- **Persistence**  
  Uses Jetpack DataStore to save blocked list, quotas, and PIN even after restart.

---

## 📸 Screenshots

| Home Screen | Settings Screen | PIN Lock |
|-------------|-----------------|----------|
| ![Home](docs/home.png) | ![Settings](docs/settings.png) | ![PIN](docs/pin.png) |

---

## 🚀 Getting Started

### 1. Clone the project
```bash
git clone https://github.com/solpokus/apps-tv-limit.git
cd apps-tv-limit
