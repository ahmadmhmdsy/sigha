<!-- Repository copy of the Sigha privacy policy. The app renders
     app/src/main/assets/legal/privacy-policy.md; the two files must stay identical.
     Host this file at a public HTTPS URL and enter that URL in Play Console (blocker B2). -->

# Sigha privacy policy

**Effective date:** 17 November 2026
**Publisher:** the Sigha publisher (legal name to be completed before the first public release).
**Contact:** privacy@sigha.app (placeholder address; the publisher must create and monitor this mailbox before the first public release).

Sigha is a file converter for Android. This policy explains, in plain language, what the app does with your data. The short version: **your files never leave your device, and Sigha does not collect anything about you.**

## 1. Your files are processed on your device

Every conversion — documents, images, audio, video, archives, text and OCR — runs entirely on your phone or tablet using libraries bundled in the app. Your files are never uploaded to us or to anyone else. Sigha has no server that receives files, and no copy of your files is kept anywhere except where you choose to save them.

When you save a converted file, it goes to the folder you picked, or to your device's Downloads folder. Sharing a file with another app uses Android's standard share sheet and is your action, not ours.

## 2. What we collect: nothing

Sigha does not collect, transmit or sell personal data. There is:

- no account, sign-in or profile;
- no analytics, telemetry or usage statistics;
- no advertising and no advertising identifier;
- no crash reporting or remote logging;
- no access to your contacts, location, camera, microphone, call logs or photos library beyond the files you explicitly pick.

The app writes no logs containing file names, file paths or file contents.

## 3. The only network request

Sigha works offline. The one exception is the optional text-recognition (OCR) language pack, which you can download yourself:

- **What is downloaded:** Tesseract trained-data files for English and Arabic (15,400,601 and 12,603,724 bytes; the download is about 28 MB in total).
- **When:** only after you tap **Download** on the Model packs screen. Nothing is downloaded during a conversion, at first launch, or in the background.
- **Where from:** `raw.githubusercontent.com` (GitHub, Inc.), using HTTPS. The file is pinned to a specific commit and checked against a SHA-256 digest that ships inside the app; a corrupted or substituted file is rejected before it is installed.
- **What the host sees:** as with any HTTPS request, GitHub receives your device's IP address and standard request information (time, requested file, user agent). It does not receive the names, contents or metadata of your files. We receive nothing.
- **Without the pack:** every conversion that does not need OCR continues to work with no network connection at all.

If you never tap Download, Sigha makes no network requests.

## 4. Permissions and why the app asks for them

- **Internet and network state:** used only for the OCR pack download described above.
- **Storage (Android 9 and older, API 28 and below):** permission to write into the public Downloads folder, because those Android versions have no other way to save there. On Android 10 and newer, Sigha saves through the MediaStore API and the system file picker, which need no storage permission.
- **Notifications (Android 13 and newer):** used only to show the progress and completion of a conversion that keeps running while Sigha is in the background. The notification shows a status message, a progress bar and the **name of the file being converted** — never its contents. Whether it appears on your lock screen depends on your Android notification settings. You can decline the permission: conversions still work, only the progress notification is lost.
- **Foreground service:** Android requires work that continues while you are in another app to run as a foreground service with a visible notification. Sigha uses it only for that: a long conversion that you started keeps going when you switch apps.

Sigha does not request the "All files access" permission and cannot read files you have not picked. It does not ask for contacts, location, camera, microphone or call-log access.

## 5. Retention and deletion

- **Converted files:** stay wherever you saved them (Downloads or a folder you chose). Sigha does not copy them elsewhere and does not delete them.
- **OCR packs:** stored in the app's private storage and removed when you uninstall the app or clear its data.
- **Temporary files:** written to the app cache while a conversion runs and deleted afterwards; Android may also clear the cache at any time.
- **Backups:** Sigha disables Android's cloud backup, so app data is not copied to Google Drive by the system.

## 6. Children

Sigha is a general-purpose utility, not directed at children. Because the app collects no data at all, it collects no data from children.

## 7. Security

The only code Sigha downloads is verified by size and SHA-256 before it is installed, and it is stored in private app storage. Conversion libraries are bundled inside the signed app package.

## 8. Your rights

Because we hold no personal data about you, there is nothing for us to export, correct or erase. If you believe otherwise, contact us at privacy@sigha.app (placeholder address, see above) and we will answer.

## 9. Changes to this policy

If the app's behaviour changes in a way that affects this policy — for example if a future version adds a new network feature — we will update this document, change the effective date, and show the new text in the app before the change takes effect. The current version is always available in **Settings → Privacy** and in the source repository.