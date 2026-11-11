# Upstream 1.6.2m sync — map

Spec: `docs/specs/upstream-1-6-2m-sync.md`
Decision: `docs/adr/0010-upstream-1-6-2m-sync.md`

Order (all touch `MainActivity`, so run sequentially):

1. 01 native/JNI core
2. 02 per-frame Original + render epoch
3. 03 geometry snapshot + composition
4. 04 panel gate + rebate + export self-check
5. 05 batch/histogram/chip fixes
6. 06 version + device audit

Preview target: 385 / `1.6.2m-m3`. Stable remains 156.
