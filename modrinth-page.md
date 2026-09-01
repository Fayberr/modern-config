# Modern Config - Modrinth page copy

Source of truth for the text on https://modrinth.com/mod/modern-config.
Update here first, then run:

    modrinth copy modern-config modrinth-page.md

The `## description` and `## body` sections are what gets uploaded.
The `#` title line and the parenthetical note below are local only.
Voice: short, concrete, no config-key dumps, no "and more". No em-dashes.

## description (short summary line)

Config screens for Fabric mods: one builder line per option, live preview while the screen is open, and a bridge that renders Cloth Config screens in the same look.

## body (full page)

# Modern Config

Modern Config gives mods a settings screen that is one scrolling column of cards instead of the usual dense panel. Changes preview live while the screen is open, and Cancel or ESC puts every value back the way it was. Mods built for it fall back to their own screens when it is absent, so it stays optional.

## Screens

- Every option is a card: pill toggles, sliders, text fields, cycle buttons, dropdowns, keybind fields and list editors.
- Options that declare a default get a reset button, shown while the value differs from it.
- Tabs split large configs into pages; the tab strip scrolls when there are more tabs than fit.
- Tooltips on options that declare one.
- `/modernconfig demo` opens a screen with one of every option type.

## Cloth Config bridge

When Cloth Config is installed, its screens are translated into the same look instead of Cloth's UI. Booleans, sliders, text and number fields, dropdowns, enums, keybinds with modifiers, color fields, string and number lists, groups and static text all come across. Entries the bridge cannot translate are skipped and logged rather than rendered as dead controls.

## For mod developers

Build a screen with the builder:

```java
ModernConfigScreen.builder(title, parent, onSave)
    .category("General")
    .bool("Enabled", () -> state.enabled, v -> state.enabled = v, true)
    .intSlider("Range", () -> state.range, v -> state.range = v, 0, 64, 1)
    .build();
```

- Colors, cycles, dropdowns, keybinds, string and number lists, text fields, notes and corner buttons are also in the builder. Optional default values add reset-to-default buttons.
- Setters run on every input, so changes preview live while the screen is open. Options snapshot when the screen opens; Cancel or ESC restores them, and Save runs your onSave.
- Existing builder calls keep working across all 1.x releases; new options arrive as additional methods.
- Add it as an optional, compileOnly dependency (`"net.fayber:modern-config"` from `mavenLocal()` today, `maven.modrinth:modern-config` from the Modrinth maven once approved) and never bundle the jar. Requires the Modern GUI library as a separate dependency at runtime; the only other dependency is Fabric API.
- Keep the screen optional with the router pattern: check `FabricLoader.isModLoaded("modernconfig")`, open your Modern Config screen, fall back to a Cloth Config screen, else open nothing. Keep each screen in its own class so its imports only classload after the check passes.
- Point Mod Menu at the same router: `getModConfigScreenFactory()` returns it, and the Waypoints mod ships a complete reference implementation in its compat package.
- GPL-3.0-or-later. The full developer guide is in the README on the source page.
