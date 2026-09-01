# Fayber Config - Modrinth page copy

Source of truth for the text on https://modrinth.com/mod/fayber-config.
Update here first, then run:

    modrinth copy fayber-config modrinth-page.md

The `## description` and `## body` sections are what gets uploaded.
The `#` title line and the parenthetical note below are local only.
Voice: short, concrete, no config-key dumps, no "and more". No em-dashes.

## description (short summary line)

Config screens for Fayber's mods, with a bridge that gives Cloth Config screens the same look.

## body (full page)

# Fayber Config

Fayber Config gives mods a settings screen that is one scrolling column of cards instead of the usual dense panel. Changes preview live while the screen is open, and Cancel or ESC puts every value back the way it was. Mods built for it fall back to their own screens when it is absent, so it stays optional.

## Screens

- Every option is a card: pill toggles, sliders, text fields, cycle buttons, dropdowns, keybind fields and list editors.
- Options that declare a default get a reset button, shown while the value differs from it.
- Tabs split large configs into pages; the tab strip scrolls when there are more tabs than fit.
- Tooltips on options that declare one.
- `/fayberconfig demo` opens a screen with one of every option type.

## Cloth Config bridge

When Cloth Config is installed, its screens are translated into the same look instead of Cloth's UI. Booleans, sliders, text and number fields, dropdowns, enums, keybinds with modifiers, color fields, string and number lists, groups and static text all come across. Entries the bridge cannot translate are skipped and logged rather than rendered as dead controls.

## For mod developers

Build a screen with the builder:

```java
FayberConfigScreen.builder(title, parent, onSave)
    .category("General")
    .bool("Enabled", () -> state.enabled, v -> state.enabled = v, true)
    .intSlider("Range", () -> state.range, v -> state.range = v, 0, 64, 1)
    .build();
```

- Colors, cycles, dropdowns, keybinds, string and number lists, text fields, notes and corner buttons are also in the builder. Optional default values add reset-to-default buttons.
- Requires the Fayber GUI library as a separate dependency; the only other dependency is Fabric API.
- GPL-3.0-or-later.
