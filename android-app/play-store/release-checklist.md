# Google Play closed-test release

Play Console draft package ID: `com.Turbidimeter.main`. Earlier direct-install prototypes used `org.citizenscience.turbimeter`, so they are a separate Android app. Export any CSV from the old app before uninstalling it. The Play app package ID cannot change after the first upload.

## Prepared in this repository

- Android 16 / API 36 target, as required for new Play submissions from 31 August 2026.
- Version 0.4 / code 4; release AAB build task: `./gradlew bundleRelease`.
- No internet or location permission in the release app; debug bridge removed.
- No upload button, contributor form, or illustrative screen preview. The app reads the sensor and keeps local history.
- In-app Privacy page and local-data deletion control.
- Store listing text, correctly sized icon and feature graphic, and genuine emulator screenshots in `assets/`.
- Public privacy policy: https://alexmanuelrivera.com/Turbimeter_Citizen_science/privacy-policy.html
- Play Console contact: cubanwaterlab@gmail.com. Ads, app access, adult audience, Data safety, government, finance, and health declarations are saved.
- Signed Play AAB: `outputs/WaterLab-Turbidimeter-0.4-play.aab` at the repository root. The private upload key is stored outside the repository in `~/.local/share/waterlab-turbidimeter-play/upload-key.p12`; its password is stored in macOS Keychain.

## Remaining for the closed test

1. Accept IARC's terms in Play Console and complete the age-rating questionnaire.
2. Upload the icon, feature graphic, and at least two screenshots, then save the store listing. The listing text is saved as a draft.
3. Upload the signed AAB to a closed-test track and enroll in Play App Signing.
4. Add tester accounts, distribute the opt-in link, and have at least 12 testers opt in for 14 continuous days before requesting production access.
5. Test the release on a phone with the actual sensor. Verify USB permission, measurement, History, CSV share, privacy page, deletion, and app restart. A calibrated result must be validated against reference standards before the listing claims quantitative turbidity.

Online API upload and location capture are not implemented in this version. They must not be claimed in the Play listing or Data safety form.

`testers-template.csv` documents the Play import format. Fill the local `testers.csv` in this directory: replace each placeholder with one tester's Google account email address, one address per line, with no header or commas. Play Console rejects UTF-8 files with a byte-order mark. `testers.csv` is ignored by Git so personal email addresses are not published in the public repository.
