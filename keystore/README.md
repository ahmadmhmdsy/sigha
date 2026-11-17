# Signing keys (git-ignored)

`keystore.properties` and every `*.jks`/`*.keystore` file are ignored by git (see the root
`.gitignore`). Nothing in this directory is committed.

## What is here now

`local-release.jks` + `keystore.properties` are a **throwaway local verification key**
(alias `sigha-local`, password `changeit`, RSA 4096, 10000 days). They exist so
`:app:assembleRelease` and `:app:bundleRelease` can be exercised and `apksigner` can prove the
release artefacts are signed. **Never upload anything signed with this key to Google Play.**

## The real upload key (publisher's job — blocker B1)

The Play upload key is the publisher's identity: only the account owner should generate and keep it.
On the publisher's machine:

```powershell
keytool -genkeypair -v -keystore sigha-upload.jks -alias sigha-upload `
  -keyalg RSA -keysize 4096 -validity 10000
```

Then replace `keystore/keystore.properties` with:

```properties
keyFile=sigha-upload.jks
storePassword=<store password>
keyAlias=sigha-upload
keyPassword=<key password>
```

and put `sigha-upload.jks` in this directory (or change `keyFile` to an absolute path).

- Back up the keystore and passwords somewhere durable and offline. Losing the upload key means
  losing the ability to update the app under the same listing (Play App Signing can reset an
  upload key, but only after identity verification).
- Play App Signing: when the first AAB is uploaded, Play can manage the app signing key; the local
  key then only signs uploads.
- The release build reads this file in `app/build.gradle.kts`; if it is missing, release builds
  fall back to unsigned/debug behaviour and the verification gate reports it.
