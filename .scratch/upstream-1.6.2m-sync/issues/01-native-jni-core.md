# 01: Native and JNI core for 1.6.2m

**Type:** task
**Blocked by:** None
**Status:** completed

**Resolution:** `CMakeLists.txt`, `spektralab_jni.cpp` and
`engine/SpektraLabEngine.java` taken from `1.6.2m`; donor switched to
`SpektraLab-1.6.2m-arm64-v8a.apk`; the old `throw_runtime(env, msg)` format
string regression was repaired. `align16k` + `ApkPolicy` 15/15 pass; JPEG
import/render verified on device. Full export re-check remains in 06.

**What to build:** Take the upstream `1.6.2m` native/JNI delta into the fork
without adopting its `MainActivity`.

- Copy `app/src/main/cpp/CMakeLists.txt` and `spektralab_jni.cpp` from
  `upstream-android/master`, keeping the fork's manifest hardening and JNI
  symbol names.
- Take the `engine/SpektraLabEngine.java` contract additions.
- Keep the donor APK path, 16 KB alignment and `check-apk.py`.
- Run a JPEG import/render/export on the device.

**Acceptance**

- [ ] `align16k` builds a signed APK; `check-apk.py` passes.
- [ ] JPEG import, film render and full-size export work on the device.
- [ ] No new exported component or permission appears in the merged APK.
