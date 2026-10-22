# Modern Config

> Work in progress: this library is still in development. Expect API changes and unfinished screens. Cloth Config remains the stable option for player-facing settings.

Config screens for Fabric mods. Each option is a card in one scrolling column; changes preview
live while the screen is open, and Cancel or ESC puts every value back the way it was. Options
are declared with a one-line builder call each. A bridge can also render existing Cloth Config
screens in the same look.

This page is written for mod developers. Players only need the jar: it requires the
[Modern GUI](https://modrinth.com/mod/modern-gui-lib) library next to it (declared as a
dependency in the mod metadata), and `/modernconfig demo` opens a screen
with one of every option type.

## Adding it to your mod

The artifact is `net.fayber:modern-config`. For local development, it comes from
mavenLocal on machines where Modern Config was built with `./gradlew publishToMavenLocal`:

```groovy
repositories {
    mavenLocal()
}

dependencies {
    // compileOnly: your mod compiles against the API without shipping it
    compileOnly "net.fayber:modern-config:1.9.5+26.2"
    // runtimeOnly pulls it into the dev client, so the integration is exercised on every boot
    runtimeOnly "net.fayber:modern-config:1.9.5+26.2"
}
```

Public Maven distribution is not available yet. Build both libraries locally using the steps below.

Declare the mod optional in your `fabric.mod.json` so your screen becomes a fallback target
rather than a hard requirement:

```json
"suggests": {
    "modernconfig": "*"
}
```

Use `depends` instead if your mod requires the screen. Your mod never needs to depend on
Modern GUI itself and should not bundle either jar.

## Building a screen

```java
public static Screen create(Screen parent) {
    return ModernConfigScreen.builder(Component.literal("My Mod Settings"), parent, () -> MyConfig.save())
        .category("General")
        .bool("Enabled", () -> MyConfig.enabled, v -> MyConfig.enabled = v, true)
        .intSlider("Range", () -> MyConfig.range, v -> MyConfig.range = v, 0, 32, 1)
        .tooltip("How far the effect reaches.")
        .cycle("Mode", () -> MyConfig.mode, v -> MyConfig.mode = v, Mode.values(), Mode::label)
        .build();
}
```

One call per option, one line each. The full set: `bool`, `intSlider`, `floatSlider`,
`doubleSlider`, `text`, `color`, `cycle`, `select`, `keybind`, `stringList`, `intList`,
`floatList`, `doubleList` and `button`. `note(...)` adds a static paragraph of documentation
text, `tab(...)` splits the screen into pages, `cornerButton(...)` pins a small secondary
action to the bottom right of the window, and `tooltip(...)` attaches to the option added just
before it. Overloads that take a default value add a reset button to the card while the current
value differs from that default. `ModernConfigScreen.Builder` in the source lists every method
with its javadoc.

Two entry kinds have richer editors: `color` renders a live swatch next to the hex field, and
clicking the swatch opens a full HSV color picker (saturation/value square, hue and alpha bars,
hex field; dismissing it restores the color the row had when it opened). The four `*List`
entries render one text field per item with a remove button and an add row at the bottom, so a
list reads as a list; invalid items are marked red and are never written until every line
parses.

Four fluent calls attach to the option added just before them: `tooltip(...)`, plus three
per-option states. `requiresRestart()` shows a Restart badge on the card (the setter still
writes through live; the badge only tells the player a restart is needed to apply it).
`enabledWhen(condition)` greys the row out and ignores its input while the condition is false,
which is how dependent options read ("Beam Width" only while the beam is enabled); the
condition is re-checked every frame and may read other options' getters. `error(supplier)`
turns the card border red and shows the message in the option's tooltip while the supplier
returns one, and falls back to the entry's own tooltip when it does not.

Every text also has a `Component` overload: labels, tooltips, tab and category names, notes
and the corner button label, plus `cycle`/`select` namers that return components. Pass
`Component.translatable("mykey")` and the text is stored as given, so it re-resolves against
the player's language whenever it is drawn (the String overloads wrap in a literal and stay
the short way to write a screen). The screen's own strings (Cancel, Save, the Restart badge,
reset-to-default) come from the `modernconfig` lang file, so they localize too.

The behavior contract, which is the point:

- Setters run on every input event, so changes preview live while the screen is open. Keep
  setters cheap (field assigns); nothing expensive on the input path.
- Every option snapshots its value when the screen opens. Cancel or ESC writes the snapshots
  back, so nothing persists unless Save is pressed.
- Save runs your `onSave` (where you persist the config) and closes.

## API stability

One-line builder calls keep working across all 1.x releases. Existing `Builder` methods keep
their signatures through 1.x; new entry kinds and options arrive as additional methods, and
anything that would break a call site waits for 2.0.0. The runtime behavior contract above
(live write-through, snapshot on open, Cancel/ESC restores, Save runs `onSave`) is held to the
same promise.

## The router pattern

Modern Config is optional, and the usual setup treats it that way: use it when it is
installed, fall back to a Cloth Config screen, and show no screen when neither is present.
Two rules make that safe:

- Check `FabricLoader.getInstance().isModLoaded("modernconfig")` before any library class is
  referenced, and keep each screen in its own class. The check guards the first reference, so
  the library classes only classload after it passes.
- Keep the dependency compileOnly and never bundle the jar; the copy in the player's mods
  folder is the one your code links against at runtime.

```java
public final class ConfigScreenRouter {
    @Nullable
    public static Screen create(Screen parent) {
        if (FabricLoader.getInstance().isModLoaded("modernconfig")) {
            return MyModernScreen.create(parent);
        }
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return MyClothScreen.create(parent);
        }
        return null;
    }
}
```

The usual setup keeps each screen in its own class: `compat/ConfigScreenRouter.java`
picks the screen and a `*ModernScreen.java` binds the config fields through the
builder, with the same router wired into both Mod Menu and an in-game Settings
button.

## Mod Menu

One entrypoint method is all Mod Menu needs; return the router so the icon and your own
settings button always agree:

```java
public class MyModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreenRouter::create;
    }
}
```

```json
"entrypoints": {
    "modmenu": [ "com.example.MyModMenu" ]
}
```

## Requirements

- Minecraft 26.2 (Fabric Loader 0.19.3+), Java 25.
- Modern GUI as a runtime dependency of Modern Config itself; Fabric API.
- Mod Menu optional, for the config button.
- Licenses: Modern Config GPL-3.0-or-later, Modern GUI LGPL-3.0-or-later.

## Conditional options

Chain `.visibleWhen(() -> condition)` after a native builder entry to hide its row until the condition is true. Hidden rows collapse and do not receive input. Conditions combine with the active tab and update live.

The Cloth bridge also preserves display requirements dependent on enum/selection values. Selector edits update the Cloth entry's UI state without calling its save consumer, so a dependent slider appears immediately while Save and Cancel retain their usual behavior.

Client smoke: with Cloth Config in `run/mods`, run `JAVA_TOOL_OPTIONS=-Dmodernconfig.previewConditional=true ./gradlew runClient -PmcPreview=true` under a display. It checks selector-driven visibility, retaining a hidden slider's count, staged edits, Cancel, and Save, then exits with `CONDITIONAL_SMOKE_PASS` in the log.

## Build from a fresh checkout

Use JDK 25 and the included Gradle wrapper. Select the same Minecraft branch in both
[Modern GUI](https://github.com/Fayberr/modern-gui) and
[Modern Config](https://github.com/Fayberr/modern-config): `fabric/26.1.x`, `fabric/26.2` or `fabric/26.3`.

1. In Modern GUI, run `./gradlew build publishToMavenLocal`.
2. In Modern Config, run `./gradlew test build publishToMavenLocal`.

On Windows, use `gradlew.bat`. Artifacts are written to `build/libs`. The 26.2 and 26.3
Maven coordinates include the Minecraft version suffix; see `gradle.properties` and
the `publishing` block in `build.gradle`. The jar's mod version has no Minecraft suffix.
