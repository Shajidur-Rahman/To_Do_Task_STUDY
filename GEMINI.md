# Chronos HSC: Smart Study Engine — Workspace Rules & Auto-Update Protocol

## ⚡ PERMANENT RULE FOR ALL FUTURE UPDATES & BUILDS (EXECUTE AUTOMATICALLY WITHOUT PROMPTING)

Every single time the user asks to update the app, add a feature, fix a bug and build, or generate a new APK, you MUST automatically execute this exact sequence without being asked:

1. **Increment `versionCode` & Bump `versionName`:**
   - In `app/build.gradle.kts`, increment `versionCode` by `+1` (e.g., `1` -> `2` -> `3`).
   - Bump `versionName` accordingly (e.g., `1.0.0` -> `1.0.1` -> `1.0.2` or `1.2.0` -> `1.2.1`).

2. **Sync `version.json` in Project Root:**
   - Update `version.json` in the root directory so that `versionCode`, `versionName`, `apkUrl`, and `releaseNotes` match the new build.
   - Remote URL for app in-app update checks: `https://raw.githubusercontent.com/Shajidur-Rahman/To_Do_Task_STUDY/main/version.json`.
   - Download URL: `https://github.com/Shajidur-Rahman/To_Do_Task_STUDY/releases/latest/download/app-release.apk` (and repo APK mirror).

3. **Cryptographically Sign with Permanent Keystore:**
   - Build the APK using the permanent keystore `app/keystore/release-key.jks` configured via `keystore.properties`.
   - Both `debug` and `release` builds MUST share this exact same signing configuration so that Android package signatures NEVER conflict and user database/data is preserved across updates.

4. **Output Exact File Paths:**
   - Always output the exact absolute and relative paths for:
     - The signed APK (`app/build/outputs/apk/release/app-release.apk` / `Chronos_HSC_Smart_Study_Engine.apk` / `release/app-release.apk`)
     - The updated `version.json` file.
   - Commit & push both code and signed APK to GitHub on every app update.
