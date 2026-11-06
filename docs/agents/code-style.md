# Code style and layering

The repository follows the Alibaba Java coding guidelines where they apply to
an Android app. `config/checkstyle/checkstyle.xml` is the executable subset;
`./gradlew checkstyleGovernance` runs it on the extracted packages and
`MainActivity`.

## Enforced now

- Type, method, member, constant, local and parameter names use the standard
  Java conventions.
- Every `if`, `else`, `for`, `while` and `do` uses braces.
- No empty blocks.
- Lines are at most 120 characters; methods at most 80 lines; nesting at most
  three levels; cyclomatic complexity at most 15.
- Fields in governed packages are private. Constants may be `static final`.
- Public classes carry class-level Javadoc.

Governed packages: `app.spektralab.core`, `app.spektralab.model`,
`app.spektralab.ui`, `app.spektralab.ui.widget`, and `MainActivity`.

## Layering

| Package | Holds | Does not hold |
|---|---|---|
| `core` | Framework-free arithmetic and rules | Android views, engine calls |
| `model` | Immutable records and the per-Frame session data | UI state, rendering |
| `ui` | Presentation state (`UiState`) | Look or engine values |
| `ui.widget` | Reusable custom views | Panel orchestration or rendering |
| `app.spektralab` | Composition root and the domain seams | A second copy of a Look value |

A new pure rule belongs in `core`; a new record in `model`; a new custom view in
`ui.widget`. Do not add a field to `MainActivity` when an existing seam owns it.

## Extracting a method

- Move the block; do not retype it. Re-typing is how the Date Back corner and
  style rows disappeared during the governance refactor.
- Before committing a split, compare the call multiset of the old and new
  bodies: every `addView`, listener, choice row and render call must appear
  exactly once, in the same order.
- Every public choice list in `DateBack` and `RebateControls` must stay inside
  an `edgeChoiceRow(...)` call. `ChoiceRowCoverageCheck` fails `checkJvm` when a
  list survives only in summary text.
- After a panel split, open that panel on the device and count its rows against
  the pre-split screen; the JVM checks cannot build Android views.

## Comments

- Class and method Javadoc is English. User-visible text stays Chinese.
- Comments explain an invariant or a non-obvious reason, not what the line does.
- A render, Look or theme rule with a second caller gets a Javadoc on the seam,
  not a comment at each call site.

## Commands

```bash
./gradlew checkstyleGovernance
./gradlew checkJvm
./gradlew lintDebug
```

CI runs all three before the APK build. Do not weaken a rule to make a local
change pass; move the code to the package that owns it.
