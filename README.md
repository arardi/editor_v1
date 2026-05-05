# Za Editor

Za Editor adalah aplikasi Android video editor berbasis Kotlin + Jetpack Compose.

## Build lokal

Project ini tidak menyertakan Gradle Wrapper karena repository ini tidak boleh berisi file binary seperti `gradle-wrapper.jar`.
Gunakan Gradle yang terinstall di sistem lokal atau Gradle yang disiapkan oleh GitHub Actions.

```bash
gradle assembleDebug
gradle assembleRelease
```

## Membuat release otomatis ke GitHub Release

Buat dan push tag versi dengan format `v*`:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Saat tag `v*` dipush, GitHub Actions akan:

1. build debug dan release APK,
2. upload APK sebagai Actions artifact bernama `ZaEditor-APK`,
3. menyalin APK release menjadi `ZaEditor-${TAG}.apk`,
4. membuat atau memperbarui GitHub Release menggunakan GitHub CLI `gh release`,
5. upload APK release asli ke GitHub Release.

## Manual release dari GitHub Actions

Workflow `Android APK Build` juga bisa dijalankan manual melalui `workflow_dispatch`.
Jika input `release_tag` diisi, misalnya `v1.0.1`, workflow akan upload APK release ke GitHub Release tag tersebut.
Jika input dikosongkan, workflow hanya membuat Actions artifact.

## Catatan Signing

Release saat ini memakai **dev signing** (debug keystore) agar build release tetap jalan tanpa secrets.
Untuk production, ganti dengan signing keystore release dari GitHub Secrets.
