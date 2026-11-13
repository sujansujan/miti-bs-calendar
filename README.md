# Miti — Plaintext Bikram Sambat (BS) Calendar for Android

**Miti (`np.com.miti.patro`)** is a 100% offline Nepali Bikram Sambat (BS) calendar, BS ↔ AD date converter, lunar Tithi & festival tracker, and repeating annual BS / Shraddha event reminder app for Android, styled after [plaintextsports.com](https://plaintextsports.com/) with literal `-------` and `|` ASCII borders, **Nepali Lokta Kagaj & Simrik Crimson** (Alucard Light), and **Dracula** (Dark) palettes.

## Key Features

1. **100% Offline BS ↔ AD Engine (`1970–2100 BS`)**:
   - O(1) prefix-sum lookup table (`BsCalendarData.kt`, `BsDateConverter.kt`) anchored to `2000-01-01 BS == 1943-04-14 AD`.
2. **True Amanta Lunar Month (*Chandra Maas*), *Tithi Kshaya / Vriddhi*, & *Adhik Maas* Engine (`TithiCalculator.kt`, `NepaliFestivals.kt`)**:
   - Evaluates Sunrise Tithi (`05:45 NST`) and *Aparahna* Tithi (`14:00 NST` for *Shraddha*) via Jean Meeus's astronomical equations.
   - Detects **Tithi Kshaya** (when 2 Tithis fall on the same solar day, e.g. `एका/द्वा`), **Vriddhi Tithi** (when 1 Tithi spans 2 sunrises, preventing duplicate consecutive-day festivals), and **Adhik Maas (`मलमास`)** (when 2 *Aunsis* fall in the same solar BS month).
   - Resolves lunar festivals by their true **Amanta Lunar Month (`shuddhaLunarMonth 1..12`)** so cross-month festivals (like *Janai Purnima* on `भदौ १, २०८४` or *Vijaya Dashami* in Kartik) and 31/32-day months never miss or duplicate festivals.
3. **Monoline Nepali Woodblock Cell Artworks (`FestivalCellArt.kt`)**:
   - Procedural Jetpack Compose `Canvas` monoline watermarks for each day of **Dashain** (*Ghatasthapana*, *Fulpati*, *Maha Ashtami*, *Maha Nawami*, *Vijaya Dashami*), all 5 days of **Tihar** (*Kaag*, *Kukur*, *Gai/Laxmi Puja*, *Gobardhan/Mha Puja*, *Bhai Tika*), personal **Birthday** and **Shraddha** (`isShraddha`) events, and major Nepali festivals.
4. **Repeating Solar BS & Lunar *Shraddha* Events, Google Calendar Sync, & JSON Backup**:
   - Schedule repeating events by **BS Date (`गते`)** or **Shraddha Lunar Tithi (`१..३०`)**, export multi-year occurrences to Google Calendar, and export/import all settings and events via bounded JSON backup.
