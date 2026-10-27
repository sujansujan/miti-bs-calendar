# Miti (मिति) — Plaintext Bikram Sambat Calendar for Android

**Miti** (`np.com.miti.patro`) is an offline-first Bikram Sambat (BS) monthly calendar and annual BS event reminder app for Android. Its visual language is inspired by [plaintextsports.com](https://plaintextsports.com/) and styled with the official [Alucard (Light) and Dracula (Dark)](https://draculatheme.com/spec) color palettes.

## Key Features

- **Annual BS Repeating Events**: Events are created on a Bikram Sambat month (`1..12`, Baisakh–Chaitra) and day (`1..32`). Each event automatically repeats on the same BS month and day in every upcoming BS year—following the Bikram Sambat calendar rather than the Gregorian calendar.
- **Sensible Variable Month-Length Handling (29–32 Days)**: Because BS month lengths vary from year to year, an event created on day `30`, `31`, or `32` may encounter a future BS year where that month has fewer days. Miti provides three configurable strategies in Settings:
  1. **Clamp to month end (Default — `CLAMP_TO_LAST_DAY`)**: Triggers on the final day of that same BS month (for example, Jestha 32 clamps to Jestha 31 in a 31-day Jestha year) and displays a `[~BS day 32]` indicator on the event card.
  2. **Roll to next month (`ROLL_TO_NEXT_MONTH`)**: Overflows into day 1 (or 2) of the following BS month.
  3. **Exact day only (`SKIP_YEAR`)**: Triggers only in BS years where that month contains the exact day number.
- **Daily Lunar Tithi (Aunsi to Purnima)**: Every day cell displays its prevailing lunar Tithi at Nepal sunrise (`UTC+05:45`), from Pratipada through Purnima (`○` full moon) in Shukla Paksha and Pratipada through Aunsi (`●` new moon) in Krishna Paksha, computed offline via Jean Meeus's astronomical equations (*Astronomical Algorithms*, Chapters 25 & 47) paired with verified Nepal Panchanga Nirnayak Samiti sunrise overrides.
- **Saturdays & Major Nepali Holidays**: Saturdays (`SAT` / `शनि`) are automatically marked as public holidays in red (`#CB3A2A` in Alucard Light, `#FF5555` in Dracula Dark), alongside fixed BS solar holidays (New Year / Biska Jatra, Republic Day, Constitution Day, Maghe Sankranti, Prajatantra Diwas, Tamu Lhosar, Prithvi Jayanti) and lunar festivals (Buddha Jayanti, Janai Purnima, Gai Jatra, Krishna Janmashtami, Haritalika Teej, Indra Jatra, Dashain, Tihar, Chhath Parva, Yomari Punhi, Sonam Lhosar, Saraswati Puja, Maha Shivaratri, Gyalpo Lhosar, Fagu Purnima / Holi, Ghode Jatra).
- **100% Offline Architecture**: Requests zero network permissions (`INTERNET` is not present in `AndroidManifest.xml`). All BS↔AD conversions (1970–2100 BS), Tithi calculations, Room SQLite persistence, and `AlarmManager` notifications run locally on the device.
- **Minimal Plaintext UI**: Uses monospace tabular grids, hairline ASCII-inspired box cards, and bracketed text actions (`< Ash`, `Kar >`, `[Today]`, `[+Event]`, `[Cfg]`). The app name (`Miti`) and theme names (`Alucard` / `Dracula`) never appear on the calendar screens; theme controls live exclusively in Settings.
- **Nepali Cultural Adaptive Icon**: Vector-based Android adaptive icon (`ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`) inspired by Newar *Ankhi Jhyal* (carved mortise-and-tenon wooden lattice window) geometry framing the *Chandra-Surya* (crescent moon and 8-rayed sun) over a flat *Dachi Appa* terracotta brick background (`#8C2D19`).

## Tech Stack & Architecture

- **Language & UI**: Kotlin 2.0.21, Jetpack Compose (Material 3 base with custom `MitiTheme` & `PlaintextTypography`), `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`.
- **Persistence**: Room SQLite (`AppDatabase`, `EventDao`, `EventEntity`) for repeating BS events; Jetpack DataStore Preferences (`UserPreferencesRepository`) for user settings.
- **Offline Reminders**: `AlarmManager` (`ReminderScheduler`) + `ReminderReceiver` + `BootCompletedReceiver`. When an event alarm fires for BS year `Y`, `ReminderReceiver` immediately computes the exact Gregorian date for BS year `Y + 1` using `BsDateConverter` and schedules the next year's alarm.

## Zero-Config Setup & GitHub Actions Guide (No Keystore or Secrets Needed)

Both the `debug` and `release` build types in `app/build.gradle.kts` use Android's built-in default debug signature (`signingConfig = signingConfigs.getByName("debug")`), so Android's package installer accepts the APK directly without requiring you to create a keystore or configure any GitHub Secrets.

### 1. Push the Project to GitHub

```bash
cd miti-bs-calendar
git init
git branch -M main
git add .
git commit -m "Initial commit: Miti BS Calendar Android app"
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

Every `push` or `pull_request` to `main`/`master` (or manual click on **Actions → Android CI & Release → Run workflow**) automatically:
1. Sets up JDK 17 and Gradle 8.9.
2. Runs unit tests (`./gradlew testDebugUnitTest`) verifying BS↔AD conversions, Tithi calculations, and variable month-length clamping/rollover.
3. Compiles the minified, installable APK (`miti.apk`) and uploads it as the `miti-apk` workflow artifact.

### 2. Optional: Publish to GitHub Releases via Version Tag

If you want a direct download link on your repo's **Releases** page (no secrets needed), push any `v*` tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

### 3. Download & Install the APK

- **From any commit / PR / manual workflow run**:
  1. Go to your repository's **Actions** tab on GitHub.
  2. Click the latest completed **Android CI & Release** workflow run.
  3. Scroll down to **Artifacts** and download **`miti-apk`**.
  4. Unzip the downloaded archive, transfer `miti.apk` to your Android phone, and tap to install.
- **From a tagged release (`v1.0.0`)**:
  1. Go to your repository's **Releases** section on GitHub.
  2. Tap **`miti-v1.0.0.apk`** under **Assets** directly on your phone to download and install.

## Data Verification Checklist (Official Sources)

Before relying on dates for legal, civil, or religious ceremonies, verify the following items against the **Nepal Panchanga Nirnayak Samiti** official gazette or **Hamro Patro**:

1. **BS Month Lengths for Future Years (2085–2100 BS)**:
   - Years `1970–2084 BS` are cross-checked against historical Kathmandu Metropolitan City / Panchanga Nirnayak Samiti records (`2000-01-01 BS = 1943-04-14 AD`, `2083-01-01 BS = 2026-04-14 AD`, `2083-07-09 BS = 2026-10-26 AD`).
   - Because the Government of Nepal officially gazettes exact month lengths (`29..32` days) one year in advance based on solar Sankranti timing, years `2085–2100 BS` in `BsCalendarData.kt` use pre-computed Surya Siddhanta tables and should be verified each Chaitra when the new Patro is published.
2. **Sunrise Boundary Tithis (Kshaya & Vriddhi Tithis)**:
   - `TithiCalculator.kt` computes lunar-solar elongation at `05:45 NST` (`00:00 UTC`) using Jean Meeus's astronomical equations and includes explicit Panchang overrides (`PANCHANG_OVERRIDES`) for `2083 BS`.
   - When a Tithi transition occurs within ~20 minutes of local sunrise in Kathmandu/Bhaktapur, traditional *Surya Siddhanta* and modern *Drik Ganita* can differ by 1 day on whether a Tithi is *Kshaya* (skipped) or *Vriddhi* (repeated across two sunrises).
3. **Cabinet-Declared Public Holidays & Regional Observances**:
   - `NepaliFestivals.kt` includes national holidays and major lunar festivals (Dashain, Tihar, Chhath, Lhosar, Teej, Janai Purnima, Holi, Shivaratri, Buddha Jayanti). Cabinet-declared ad-hoc holidays or province/valley-specific holidays (such as Indra Jatra, Gai Jatra, and Ghode Jatra for Kathmandu Valley) should be cross-checked with the annual Ministry of Home Affairs (*Griha Mantralaya*) Gazette notice.
