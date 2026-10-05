# Google Play closed-test release

Play Console draft package ID: `com.Turbidimeter.main`. Earlier direct-install prototypes used `org.citizenscience.turbimeter`, so they are a separate Android app. Export any CSV from the old app before uninstalling it. The Play app package ID cannot change after the first upload.

## Prepared in this repository

- Android 16 / API 36 target, as required for new Play submissions from 31 August 2026.
- Version 0.4 / code 4; release AAB build task: `./gradlew bundleRelease`.
- No internet or location permission in the release app; debug bridge removed.
- No upload button, contributor form, or illustrative screen preview. The app reads the sensor and keeps local history.
- In-app Privacy page and local-data deletion control.
- Store listing draft and correctly sized icon and feature graphic in `assets/`.

## Still required before a Play submission

1. Use the verified Play Console account. Confirm the publisher name and public contact email.
2. Publish `docs/privacy-policy.html` at a public URL, then enter that URL in Play Console. Check that the in-app policy matches the release build.
3. Create and safely store a private upload key outside the repository. Sign the AAB and enroll in Play App Signing. Never commit the keystore or passwords.
4. Enter the store listing text, icon, feature graphic, and at least two genuine phone screenshots. State that a compatible external USB-C sensor is required and that readings stay local.
5. Complete the Data safety form, content rating, target audience, ads declaration, and other Play Console app-content forms. Readings stay on-device; manual CSV sharing is user-initiated. Recheck this declaration against the final artifact.
6. Test the signed release on a phone with the actual sensor. Verify USB permission, measurement, History, CSV share, privacy page, deletion, and app restart. A calibrated result must be validated against reference standards before the listing claims quantitative turbidity.
7. Run Google's required closed test with at least 12 opted-in testers for 14 continuous days, then request production access.

Online API upload and location capture are not implemented in this version. They must not be claimed in the Play listing or Data safety form.
