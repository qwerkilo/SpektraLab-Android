# Known issues

Defects observed on the fork that are not fixed by the current tree, with the
workaround and whether they block a release. The release gate is audited by
`app/src/test/java/app/spektralab/VersionCheck.java`; keep the `Release gate:`
line in the same form the check reads.

## KI-1 · Rebate bottom print lost above ~16.7 MP

**Status:** open. **Release gate: open.**

**Affected:** the fork from `1.4.0-m3` (engine upstream `445e314`, out of
tree). The Rebate/overscan path on any export whose rebate **canvas** is
larger than **16,776,960 pixels** (65,535 workgroups × 256 threads, ~16.7 MP).

**Symptom:** the exported file is missing the bottom band of the Rebate — the
manufacturer's edge print and the film edge below the picture — while the
preview shows it. The picture area itself is complete, and the file's size and
the stage preview look normal, so the loss is easy to miss without looking at
the bottom edge of the file.

**Cause:** engine-side. The rebate draw runs as a dispatch over the canvas;
past the ~16.7 MP workgroup ceiling the final rows are not drawn into the
export buffer. The app-side canvas trim (ticket 01) crops to the engine's
top-left canvas and does not fix the lost rows. The engine is out of tree;
this sync does not change it (ADR-0005, `docs/specs/upstream-1.4-sync.md`).

**Reproduced:** 2026-11-03 on `10.110.67.69:39613` (Android 17) with
`1.4.0-m3` (code 69): CineStill 800T, 135 full frame, a 4500×3000 JPEG
source exported as **4704×4345 (20.44 MP)**. The top band prints
`CINESTILL 800T`; the bottom band has its sprocket holes but neither the frame
number nor the `2A` edge print. The same Look at 3156×2897 (9.14 MP) keeps the
bottom print, so this is the >16.7 MP case, not a general rebate loss.

**Workaround:** keep the exported rebate canvas at or under 16,776,960 px.
The export snackbar prints the canvas as `画布 WxH` — if that product is over
the ceiling, either turn the Rebate off for this export (the picture is
complete; only the edge is lost) or export from a Frame whose Gate/Scale keeps
the canvas under the ceiling. The preview is not affected, so the preview
cannot be used to decide whether the file is good: inspect the exported
file's bottom edge.

**Release gate:** a build with this defect may not be announced as `stable`.
`update.json` must stay off the `stable` channel while this line reads
`Release gate: open`. Change it to `Release gate: accepted` only after an
explicit acceptance is recorded in ADR-0005, or after the engine fix ships and
a >16.7 MP rebate export with the bottom band present has been verified on a
device. `VersionCheck` fails the build if the channel and this line disagree.
`scripts/announce.sh` writes `"channel": "stable"` unconditionally, so do not
run it while this gate is open.

**Verification for the eventual fix:** import a frame, enable the Rebate, set a
Gate/Scale whose exported canvas is over 16,776,960 px, export, and confirm the
bottom edge print is present in the file (not only in the preview). Record the
case in `docs/testing/manual-device-checklist.md`.
