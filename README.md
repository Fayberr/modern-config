# Modern Config

Config screens for Fabric mods. Each option is a card in one scrolling column; changes preview
live while the screen is open, and Cancel or ESC puts every value back the way it was. Options
are declared with a one-line builder call each. A bridge can also render existing Cloth Config
screens in the same look.

This page is written for mod developers. Players only need the jar: it requires the
[Modern GUI](https://modrinth.com/mod/modern-gui-lib) library next to it (declared as a
dependency on Modrinth, so launchers install both), and `/modernconfig demo` opens a screen
with one of every option type.

## Adding it to your mod

The artifact is `net.fayber:modern-config`. Until the Modrinth maven is live, it comes from
mavenLocal on machines where Modern Config was built with `./gradlew publishToMavenLocal`:

```groovy
repositories {
    mavenLocal()
}

dependencies {
    // compileOnly: your mod compiles against the API without shipping it
    compileOnly "net.fayber:modern-config:1.7.1"
    // runtimeOnly pulls it into the dev client, so the integration is exercised on every boot
    runtimeOnly "net.fayber:modern-config:1.7.1"
}
```

Once the Modrinth project is approved, the same coordinates are available as
`maven.modrinth:modern-config:<version>` from `https://api.modrinth.com/maven`.

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

A complete working example lives in the Waypoints mod:
`compat/ConfigScreenRouter.java` picks the screen and `compat/WaypointsModernScreen.java`
binds fifteen config fields through the builder, with the same router wired into both Mod Menu
and an in-game Settings button.

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

- Minecraft 26.1.x (Fabric Loader 0.19.3+), Java 25.
- Modern GUI as a runtime dependency of Modern Config itself; Fabric API.
- Mod Menu optional, for the config button.
- Licenses: Modern Config GPL-3.0-or-later, Modern GUI LGPL-3.0-or-later.
