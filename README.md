# Za Editor

Za Editor adalah aplikasi Android video editor berbasis Kotlin + Jetpack Compose.

## Build lokal

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Membuat release otomatis ke GitHub Release

```bash
git tag v1.0.0
git push origin v1.0.0
```

Saat tag `v*` dipush, GitHub Actions akan otomatis build APK dan upload ke GitHub Release.

## Catatan Signing

Release saat ini memakai **dev signing** (debug keystore) agar build release tetap jalan tanpa secrets.
Untuk production, ganti dengan signing keystore release dari GitHub Secrets.
