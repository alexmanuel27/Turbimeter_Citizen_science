# Google Play release handoff

Current package ID: `org.citizenscience.turbimeter`. Confirm this in Play Console before the first upload; the package ID cannot be changed for that listing. The prototype installed directly on the Samsung used this same ID with a debug key, so it may need to be uninstalled before installing the Play-signed app. Back up any CSV first.

## Prepared in this repository

- Android 16 / API 36 target, as required for new Play submissions from 31 August 2026.
- Release AAB build task: `./gradlew bundleRelease` (currently unsigned).
- No internet or location permission in the release app; debug bridge removed.
- In-app Privacy page and local-data deletion control.
- Store listing draft and correctly sized icon and feature graphic in `assets/`.

## Still required before a Play submission

1. Create and verify the Play Console developer account. Choose the final publisher name and public contact email.
2. Replace the placeholders in `privacy-policy-draft.md`, host it at a public web URL, and enter that URL in Play Console. Check that the in-app policy still matches the release build.
3. Create and safely store a private upload key outside the repository. Use Android Studio's **Generate Signed Bundle / APK** wizard to sign the AAB and enroll in Play App Signing. Never commit the keystore or passwords.
4. Enter the store listing text, icon, feature graphic, and at least two genuine phone screenshots. State that a compatible external USB-C sensor is required and that this version saves locally rather than uploading.
5. Complete the Data safety form, content rating, target audience, ads declaration, and any other Play Console app-content forms. Based on the current release code, readings and Signature data stay on-device; manual CSV sharing is user-initiated. Recheck this declaration against the final artifact.
6. Test the signed release on a phone with the actual sensor. Verify USB permission, measurement, History, CSV share, Signature, privacy page, deletion, and app restart. A calibrated result must be validated against reference standards before the listing claims quantitative turbidity.
7. If the new account is personal, run Google's required closed test with at least 12 opted-in testers for 14 continuous days, then request production access.

Online API upload and location capture are not implemented in this version. They must not be claimed in the Play listing or Data safety form.
