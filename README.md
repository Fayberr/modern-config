# Fayber Config

Slim, modern config screens for Fayber's Fabric mods (Minecraft 26.1.x, client only, zero mixins).
Dark rounded cards, anti-aliased shapes drawn at full monitor resolution, Inter UI font, live
preview while the screen is open, and Cancel/ESC that actually reverts.

**This is a library, not a config file format.** It does not read or write JSON. A mod describes
its options through a builder, the library renders them and writes changes through the mod's own
setters; persistence stays in the mod. Waypoints Mod is the first consumer; any other mod adds
the same three-line setup below.

The mod id is `fayberconfig`, the maven artifact is `net.fayber:fayber-config:1.0.0`
(published with `./gradlew publishToMavenLocal`, remapped to mojmap by Loom).

## Using it in your own mod

### 1. Dependencies (`build.gradle`)

```gradle
repositories {
    mavenLocal()
}

dependencies {
    compileOnly "net.fayber:fayber-config:1.0.0"
    runtimeOnly  "net.fayber:fayber-config:1.0.0"   // so you can test in the dev client
}
```

Loom 1.17 has no `modImplementation`; plain `compileOnly`/`runtimeOnly` work because both sides
are mojmap. Keep the library **optional** in `fabric.mod.json` so your mod still runs without it:

```json
"suggests": { "fayberconfig": "*" }
```

### 2. Build the screen

```java
Screen screen = FayberConfigScreen
        .builder(Component.literal("My Mod"), parentScreen, this::saveConfig)
        .category("General")
        .bool("Enabled", () -> config.enabled, v -> config.enabled = v)
        .tooltip("Turns the feature on or off.")
        .intSlider("Range", () -> config.range, v -> config.range = v, 0, 100, 5)
        .category("Advanced")
        .floatSlider("Scale", () -> config.scale, v -> config.scale = v, 0.2f, 3.0f, 0.05f)
        .doubleSlider("Distance", () -> config.dist, v -> config.dist = v, 1.0, 32.0, 0.5)
        .text("Name", () -> config.name, v -> config.name = v, 32)
        .button("Reset defaults", this::resetDefaults)
        .build();
this.minecraft.setScreen(screen);
```

Rules for the setters: they must be cheap field assignments, because values write through
**immediately** (a slider visibly changes the game while the screen is open). The library
snapshots every value when the screen opens; Cancel/ESC replays the snapshots through the same
setters, Save runs the `onSave` runnable, where the mod persists its own config.

### 3. Route to it safely (optional dependency pattern)

Never reference library classes unconditionally: the jar may be absent at runtime. Check
`isModLoaded` first and keep each concrete screen in its own class, only reachable from its own
branch. This is the whole pattern, as Waypoints does it:

```java
public static Screen create(Screen parent) {
    if (FabricLoader.getInstance().isModLoaded("fayberconfig")) {
        return MyModFayberScreen.create(parent);   // only classloaded when the lib exists
    }
    if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
        return MyModClothScreen.create(parent);
    }
    return null;
}
```

## The rendering style ("how does it look like that")

Everything visual lives in `net.fayber.fayberconfig.gui` and is reusable knowledge for any other
mod UI. The pixelated look of vanilla GUIs has three separate causes, and each gets its own fix.

### 1. Why vanilla looks blocky

1. **The GUI-scale grid.** Vanilla snaps every draw to GUI pixels. At GUI scale 3 a "1px" border
   is 3 screen pixels thick and a rounded corner is a visible staircase.
2. **The bitmap font.** The default font is an 8px bitmap grid with a hard 1-GUI-px drop shadow
   (a 3px black smear at scale 3).
3. None of this is the `Screen` system's fault. Feather-style UIs run on the same framework; they
   just draw differently.

### 2. Draw in physical pixels, lay out in GUI pixels

The core trick, all in `Ui.java`. Every primitive takes **float** coordinates in GUI space, then
rescales the matrix to device pixels and rounds each coordinate there:

```java
float s = (float) mc.getWindow().getGuiScale();
gfx.pose().pushMatrix();
gfx.pose().scale(1.0f / s, 1.0f / s);
gfx.fill(Math.round(x * s), Math.round(y * s),
         Math.round((x + w) * s), Math.round((y + h) * s), color);
gfx.pose().popMatrix();
```

Layout, widget bounds and mouse hit-testing stay in GUI pixels (so widgets behave exactly like
vanilla), but strokes land on the monitor's pixel grid: a half-GUI-pixel hairline is a real thin
line, not a 3px slab. Everything else in `Ui` is built from this: `rect`, `roundRect`,
`roundRectBorder`, `pill` (capsule = rounded rect with radius h/2), `circle`, `shadow`
(concentric rounded rects with rising alpha, a handful of quads, no textures).

### 3. Anti-aliased corners with an SDF shader hidden in the UVs

`GuiGraphics.fill()` emits no UVs, so it cannot carry signed-distance data to a fragment shader.
The **textured blit path does**. That is the whole trick:

- Each corner of a rounded rect is one `r x r` blit from a `2r x 2r` white texture, with the UV
  offset picked (`u, v in {0, r}`) so the quad samples exactly one quadrant of the unit square.
- The fragment shader (`shaders/core/round_corner.fsh`) reconstructs position relative to the
  corner's circle centre as `p = texCoord0 * 2 - 1`, measures `d = length(p)`, and computes
  coverage `1 - smoothstep(1 - aa, 1 + aa, d)` with `aa = fwidth(d)`: an edge exactly one screen
  pixel wide, at any GUI scale, any radius, any resolution. No per-draw uniforms, no real
  texture (a 4x4 white png keeps the declared sampler alive).
- A rounded rect is 3 plain fills (centre column + two side slabs) plus 4 corner blits; a border
  is an outer shape in border colour with the fill inset into it, so both edges stay smooth.

Practical notes that cost a day to learn, version 26.1:

- Mod pipelines need **no registration**: `GlDevice.getOrCompilePipeline` is a lazy
  `computeIfAbsent` and `ShaderManager` loads shaders from every namespace.
- Clone the pipeline state off `RenderPipelines.GUI_TEXTURED` at runtime instead of hard-coding
  state, so it tracks MC updates. But `getColorTargetState()`/`getDepthStencilState()` are
  **null** on `GUI_TEXTURED`, and the builder setters NPE via `Optional.of(null)`: null-check
  both before calling them.
- Build the pipeline lazily inside try/catch and keep a fallback (Fayber Config draws corners as
  one fill per physical pixel row, a real arc at monitor resolution, just not anti-aliased).

### 4. One TTF font, bundled once per GUI scale

This is the non-obvious one. Minecraft rasterises a TTF glyph at `size * oversample` texels,
draws it `size` GUI px tall, and samples the glyph atlas with **NEAREST** (no filtering).
One texel therefore covers `oversample / guiScale` screen pixels, and any ratio other than 1 is
an unfiltered point-sample: that is why TTF text in mods usually looks "a bit jagged" no matter
what font you ship. A single fixed `oversample` is only ever right at one GUI scale.

The fix, in `assets/fayberconfig/font/`: ship the same font once per GUI scale
(`ui_x1` .. `ui_x6` and bold variants, each with `oversample: N.0` equal to its scale) and pick
the variant at draw time with `Math.round(window.getGuiScale())`. Then it is exactly one texel
per physical pixel, and because advances and bearings are stored as `raster / oversample`, text
also lands **on** whole physical pixels instead of between them.

Version 26.1 specifics:

- Fonts are selected **per Component** through the style: `Style.withFont(...)` takes a
  `FontDescription` (`new FontDescription.Resource(id)`), not an `Identifier`, and
  `FontManager` has no public getter. `Ui.ui(...)` / `Ui.uiBold(...)` wrap strings; the screen
  keeps its `title` Component raw and re-styles it at draw time, because `Screen.title` is final
  and the correct variant is not known until the scale is.
- Turn text shadows **off** everywhere: `gfx.text(font, comp, x, y, color, false)`. The vanilla
  shadow is a hard 1-GUI-px offset copy. `centeredText` has no shadow flag, so `Ui` provides
  `textCentered`/`textRight` helpers that position manually.

### 5. Design rules

- **Neutral surface ramp, one accent.** The greys have equal R/G/B so nothing reads as tinted
  (`PANEL #121212`, `CARD #1A1A1A`, borders `#262626`, text `#F0F0F0` / `#A3A3A3` / `#6E6E6E`).
  The single colour in the whole screen is one soft blue (`ACCENT #7AA2F7`) reserved for
  interactive and confirming elements: toggle-on track, slider fill, the Save button.
- **Hover brightens, it does not recolour.** Every card/track has a hover variant one step up
  the ramp, plus a border that brightens with it.
- **Animate knobs, not layouts.** The toggle knob eases toward its target each frame
  (`pos += (target - pos) * 0.35f`), initialised directly on first draw so opening the screen
  does not animate everything at once.
- Cards are 34 GUI px tall with 12 px padding and radius 6; the panel uses radius 10 and
  0.5-px hairline separators between title bar, body and footer.

## Widget construction rules (26.1 API)

- `AbstractWidget.extractRenderState` is **final**. Custom widgets override the protected
  `extractWidgetRenderState` (sliders) or, for buttons, `extractContents`
  (`AbstractButton.extractWidgetRenderState` is final but only dispatches to `extractContents`),
  which fully replaces the vanilla sprite drawing while keeping click sounds, activation and
  narration for free.
- `AbstractSliderButton`'s constructor does **not** call `updateMessage()`; subclasses must call
  it themselves after their own fields are initialised.
- Scrolling rows with interactive children: extend `ContainerObjectSelectionList` (the vanilla
  KeyBindsList pattern). The list positions rows; each row's `extractContent` repositions its
  child widgets from the row's current coords every frame (scroll/resize-safe) and calls their
  final `extractRenderState`. `children()`/`narratables()` wire clicks, focus and keyboard;
  widgets being `GuiEventListener` and `NarratableEntry`, one list serves both dispatch paths.
- **`AbstractSelectionList.updateSize(int, HeaderAndFooterLayout)` takes the WIDTH as its int**,
  not the height. Fayber Config sidesteps this by laying out by hand with
  `updateSizeAndPosition(width, height, x, y)` instead of using `HeaderAndFooterLayout`.
- Scrollbar geometry: `scrollBarX() == getRowRight() + scrollbarWidth() + 2`, so row content must
  be narrower than the list by at least `2 * (scrollbarWidth() + 2)` or the bar lands outside the
  panel. Replace the vanilla sprite bar by overriding `extractScrollbar`; no-op
  `extractListBackground`/`extractListSeparators` (the screen draws its own panel).
- Draw panel chrome **before** `super.extractRenderState(...)`: within one stratum, extraction
  order is draw order.
- Do not put non-asset files under `src/main/resources/assets/` (MC logs
  `Invalid path in mod resource-pack`); the Inter license lives at the repo root.

## Headless design workbench

`tools/preview.sh [output.png] [gui_scale] [wait_seconds]` starts `Xvfb :97`, sets the GUI scale
in `run/options.txt`, runs `./gradlew runClient -PfcPreview=true` (which enables the
`dev/PreviewHook` demo screen via `-Dfayberconfig.preview=true`), waits for the demo screen to
open, and grabs a frame with `ffmpeg -f x11grab`. Zoom into a capture with
`ffmpeg -vf "crop=W:H:X:Y,scale=iw*N:ih*N:flags=neighbor"` (no ImageMagick needed). This is how
both the layout bug and the pipeline NPE were found without shipping a build.

## File map

| Path | What it is |
| --- | --- |
| `api/FayberConfigScreen` | The screen: panel layout, builder API, snapshot/cancel/save contract |
| `api/ConfigEntry` | One option descriptor (label, getter/setter, range, snapshot/restore) |
| `gui/Ui` | The drawing layer: physical-pixel primitives, font styles, text helpers |
| `gui/FayberPipelines` | Lazy-built round-corner pipeline + fallback |
| `gui/GuiUtil` | The palette |
| `gui/ConfigEntryList` | Scrollable card list, rows, slim scrollbar |
| `gui/FlatButton`, `PillToggleWidget`, `StyledSlider` (+ Int/Float/Double) | Widgets |
| `shaders/core/round_corner.fsh` | The SDF corner shader |
| `font/ui_x{1..6}.json`, `ui_bold_x{1..6}.json` | Inter bundled once per GUI scale |
| `dev/PreviewHook`, `tools/preview.sh` | Headless screenshot harness |

## License

GPL-3.0-or-later. Bundled Inter font: SIL Open Font License 1.1 (`INTER-OFL.txt`).
