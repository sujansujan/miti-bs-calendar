# Miti (मिति) — Plaintext Offline Bikram Sambat Calendar for Android

**Miti** (`np.com.miti.patro`) is a 100% offline Android Bikram Sambat (BS) calendar app built with Kotlin, Jetpack Compose, Room, DataStore, and `AlarmManager`.

## Core Features

1. **Annual BS Repeating Events**:
   - Add events anchored to a Bikram Sambat month (`1..12`, Baisakh through Chaitra) and day (`1..32`).
   - Events repeat automatically in every upcoming BS year following the **Bikram Sambat** calendar rather than the Gregorian calendar.
   - **Variable Month-Length Handling (`MissingDayStrategy`)**: Because BS months vary between 29 and 32 days across years, each event supports three explicit strategies when a target year's month is shorter than the event's day:
     - `CLAMP_TO_LAST_DAY` (Default): Triggers on the final day of that BS month (e.g., an event on `Baisakh 31` triggers on `Baisakh 30` in a 30-day Baisakh).
     - `ROLL_TO_NEXT_MONTH`: Triggers on the 1st day of the following BS month.
     - `SKIP_YEAR`: Triggers only in BS years where the exact day number exists.
2. **Monthly BS Calendar + Daily Lunar Tithi**:
   - Authoritative BS month-length lookup table covering **1970 BS – 2100 BS** (1913 AD – 2044 AD), anchored at `2000-01-01 BS = 1943-04-14 AD`.
   - High-precision astronomical **Tithi** calculation (`TithiCalculator`) using Jean Meeus's lunar-solar elongation algorithm evaluated at Kathmandu sunrise (`UTC+05:45`, `85.3240° E`), displaying all 30 lunar phases from **Pratipada** through **Purnima** (Full Moon `○`) and **Aunsi** (New Moon `●`).
   - **Saturdays** are highlighted as weekly holidays, alongside major Nepali solar and lunar festivals (`NepaliFestivals`: New Year, Buddha Jayanti, Janai Purnima, Teej, Dashain, Tihar, Chhath, Maghe Sankranti, Maha Shivaratri, Fagu Purnima, etc.).
3. **Plaintext Aesthetics & `-------` ASCII Table Borders**:
   - Inspired by [plaintextsports.com](https://plaintextsports.com/), every UI and UX element uses strict monospace typography, literal `+-------------+` / `-------` horizontal borders, and `|` vertical column pipes.
   - Interactive elements use plaintext brackets (`[< Bai]`, `[Jes >]`, `[TODAY]`, `[CFG]`, `[09]` selected day, `*09*` today, `[x]` / `[ ]` toggles, `(*)` / `( )` radio selectors, and `> ` terminal inputs underlined with `-------`).
   - **Alucard** (official Dracula Light: `#FFFBEB` background, `#1F1F1F` foreground) and **Dracula** (Dark: `#282A36` background, `#F8F8F2` foreground) color palettes.
   - Minimal UI: neither the app name (`Miti`) nor the active theme indicator appears on the main calendar screen—only inside the Settings screen (`[CFG]` / `[सेटिङ]`).
4. **Full Nepali Localization Everywhere**:
   - Turning on **Nepali Language & Devanagari Numerals** in Settings switches **100% of the app** into Nepali: navigation buttons (`[<<वर्ष]`, `[आज]`, `[+कार्यक्रम]`, `[सेटिङ]`), BS and AD dates, Tithi and Paksha names, festival tags, event editor dialogs, short-month rules, alarm summaries, notifications, and Settings sections.
5. **Google Calendar Sync (Offline Provider + Intent Fallback)**:
   - Because BS repeating dates fall on different Gregorian dates every year, Miti includes `GoogleCalendarSyncHelper` to project each repeating BS event across the next `3`, `5`, or `10` upcoming BS years, compute the exact Gregorian (`AD`) date for each year, and write them directly into your device's local **Google Calendar** via Android's `CalendarContract` provider (requiring **zero internet permission**; the Android OS Google Calendar sync adapter handles cloud sync).
   - Also includes a zero-permission `[+GCAL]` / `[+गुगल]` button on each event card to open the Google Calendar event sheet pre-filled for any specific year.
6. **Authentic Nepali Adaptive Icon**:
   - Vector-crafted **Newar Ankhi Jhyal & Chandra-Surya** motif with a dedicated Android 13+ monochrome layer (`ic_launcher_monochrome.xml`).
7. **Zero Signing Setup Required**:
   - Configured for personal use (`release` builds are automatically signed with the standard Android debug key), so every APK built by GitHub Actions installs directly onto your phone without needing any keystore or GitHub Secrets.

---

## Building & Installing the APK

### Option 1: GitHub Actions (Automatic)
1. Push this repository to GitHub.
2. Every push to `main`/`master`, pull request, or manual **Run workflow** click automatically runs all unit tests and compiles both `app-debug.apk` and `miti.apk` (`app-release.apk`).
3. Open the **Actions** tab on GitHub -> click the latest workflow run -> scroll down to **Artifacts** -> download **`miti-apk`**, unzip it on your phone, and tap `miti.apk` to install.
4. Optional: If you push a version tag (`git tag v1.0.0 && git push origin v1.0.0`), GitHub Actions will also attach the installable APK directly to a **GitHub Release** page.

### Option 2: Local Build
```bash
chmod +x gradlew
./gradlew testDebugUnitTest assembleRelease
```
The installable APK will be at `app/build/outputs/apk/release/app-release.apk`.
