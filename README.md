# LabAR

#### Before Using This App Source Code
Pastikan minimum sdk android version yang project kamu gunakan adalah API level 24 (Android 7.0) keatas.

#### Setup
Implement atau Install Library yang dibutuhkan pada file *build.gradle* project:

```
implementation 'com.gorisse.thomas.sceneform:sceneform:1.21.0'
implementation 'com.google.android.filament:filamat-android:1.21.1'
implementation 'androidx.fragment:fragment-ktx:1.5.5'
implementation 'com.intuit.sdp:sdp-android:1.1.0'
implementation 'com.airbnb.android:lottie:3.4.0'
```
Setelah menyalin implementation pada build.gradle project, klik Sync On untuk menginstall library.


##### Note
3D object yang bisa digunakan pada library ini hanyalah 3D objek yang berekstensi file glb atau gltf.
