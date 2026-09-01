# Fayber Config - Modrinth page copy

Source of truth for the text on https://modrinth.com/mod/fayber-config.
Update here first, then run:

    modrinth copy fayber-config modrinth-page.md

The `## description` and `## body` sections are what gets uploaded.
The `#` title line and the parenthetical note below are local only.
Voice: short, concrete, no config-key dumps, no "and more". No em-dashes.

## description (short summary line)

Slim, modern config screens for Fayber's mods, plus an optional bridge that translates Cloth Config screens into the same look.

## body (full page)

# Fayber Config

A client-side config mod with a dark, rounded-card settings UI: one scrolling column, live preview, and a Cancel button that truly restores. Mods built for it fall back to their own screens when it is absent, so it stays optional.

## Screens

- Every option is a card: pill toggles, sliders, text fields, cycle buttons, keybind capture fields, string list editors and action buttons.
- Changes preview live while the screen is open; Cancel and ESC restore the values the screen opened with.
- Tabs split large configs into pages; the tab strip scrolls when there are more tabs than fit.
- Tooltips on every option that declares one.

## Cloth Config bridge

When Cloth Config is installed, its screens are translated into the same look instead of Cloth's UI: booleans, sliders, text and number fields, dropdowns, enums, keybinds with modifiers, color fields, string and number list editors, groups and static text. Entries the bridge cannot translate are skipped and logged rather than rendered as dead controls.

## For mod developers

- Builder API: `FayberConfigScreen.builder()` with tabs, category headers, booleans, int, float and double sliders, text fields, cycles, buttons, keybinds, string lists, notes and corner buttons.
- Embeds the Fayber GUI library via jar-in-jar; the only other dependency is Fabric API.
- GPL-3.0-or-later.
