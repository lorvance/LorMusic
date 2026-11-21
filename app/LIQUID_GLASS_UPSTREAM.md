# Current BETA-2 renderer configuration

The app now targets the provided sample's AGP 9.3.3, Kotlin 2.4.20, Gradle 9.8.0 and Compose BOM 2026.09.00 (UI 1.12.1), with compile SDK 37, target SDK 35 and minimum SDK 26. Native archive layer recording/context density and current GraphicsLayerScope/Paint APIs are restored. Haze stays in its 1.x API line at 1.7.2. Historical integration notes follow for traceability.

# Liquid Glass sources

Vendored upstream components:

- Kyant0/AndroidLiquidGlass 2.0.1: backdrop engine, LiquidBottomTabs, LiquidBottomTab, LiquidSlider and LiquidToggle.
- ntvien/liquid-glass 0.3.0: LiquidGlassScene, LiquidSurface, LiquidMenuButton and LiquidSegmentedControl, including the package-private backdrop engine.
- Kyant0/shapes 1.2.1: upstream Android/common shape implementation used by the Kyant controls.

All three projects use Apache-2.0. License copies and upstream NOTICE are shipped in `src/main/assets/licenses`. Upstream package names and copyright headers are retained.

## Initial compatibility pass (historical)

The first integration used Kotlin 2.0.21 and Compose 1.7.6. BETA-2 now uses the renderer/toolchain from the supplied library example; see the current configuration below. Android expect/actual declarations are merged; framework Paint access uses asFrameworkPaint(). Newer named context parameters use a captured Density instead. Unsupported GraphicsLayerScope color-filter properties use a saveLayer Paint filter. Android uptimeMillis and Compose withFrameNanos replace newer clock helpers. Haptic constants use TextHandleMove on the existing Compose API. Integer clamping uses coerceIn().

## App integration

ModernGlassProviders provides page-owned captures for both backdrop implementations and recreates sources with the active page to avoid stale captures. The navbar, settings toggle and settings/EQ sliders use adapted Kyant controls. Music playback sliders and the wavy play/pause control keep the original LorMusic implementation. Surfaces, press-reactive play buttons, track sort/action menus use ntvien components. Navigation preview records its own backdrop. Accent colors follow app preferences.

Slider release callbacks and finite range/step validation are added for player seeking. The saved strength defaults to 90%; the legacy transparency preference is migrated inversely to preserve existing appearance. Existing ordinary blur and Material navigation paths remain available.

## Startup lifecycle fix (build 79)

Both new backdrop engines defer size-dependent effect execution until measured finite positive geometry is available. The first measured draw registers effect snapshot reads again, preserving reactive settings/shape updates after the pre-layout attach callback. Detach/reattach resets are covered by regression tests.

## 1.8.2 BETA-2 integration corrections

Removed the hidden duplicate tab-content capture. A single selection indicator retains blur and lens effects at rest. Tab taps are controlled by app state; horizontal drag commits one nearest index at release, without callback collectors feeding selection back into themselves. The indicator uses upstream DampedDragAnimation press, spring and velocity deformation. Labels follow the existing show-labels setting.

The ntvien adapters now sample actual screen captures, including search, rather than a separate theme gradient. Captures share a recording guard across engines, and source coordinates are cleared only by their owning node. Search and other glass controls use restrained common fills rather than opaque emphasis. Toggles retain the upstream 64×28dp track and 40×24dp liquid thumb, a 48dp full-track touch target, DampedDragAnimation deformation/velocity and theme colors. A 4dp movement threshold separates taps from intentional drags.

The app integration opts into GlassLook.Rich and Full shader quality on supported Android versions, overriding the library's automatic frost/low-RAM fallback only for these explicitly enabled glass controls. The music playback slider and wavy play/pause restoration remains in place.

## BETA-2 archive-fidelity correction (build 82)

The navbar now uses the original archive component again: DampedDragAnimation, moving panel recoil, InteractiveHighlight, the magnified icon-capture layer, combined backdrop lens and press-dependent shadows. The hidden capture records icons rather than a second copy of page content, avoiding ghost page text and double tinting. Its selection callback no longer feeds external selection changes back into itself. Lens/highlight remain visible at rest.

LiquidToggle retains the archive's green track (#34C759 / #30D158), white optical thumb and original press-dependent refraction/stretch. Material accent colors no longer replace its green. Sorting menu geometry and item stagger remain from ntvien; its glass body no longer fades to zero during the morph, blur is increased for readability, and the caller's saved glass power is carried into the top overlay.

Build83 removes the app-added multiplication of ntvien lensAmount by the saved strength. Lens height/amount follow the original style; the setting controls blur/fill without deleting the archive refraction.

Build84 makes capture recursion detection source-specific (`Backdrop.isBeingRecorded`, propagated through combined/decorated backdrops), so valid optical background captures inside the archive navbar are preserved. The full hidden optical capture from the original component is restored; broad global draw suppression no longer strips its blur/lens.

Build85 uses NtBackdropAdapter to share the Kyant page capture with ntvien, preventing cross-engine capture cycles and stale copies. Controls still use their original runtime-shader/track capture, with an independent themed background because they are embedded inside the recorded settings page. Source-node updates transfer attached coordinates immediately, without requiring a new layout pass.

Build86 migrates Compose1.7.6/Kotlin2.0.21 to the supplied sample toolchain (Compose BOM2026.09.00, Kotlin2.4.20, AGP9.3.3, Gradle9.8.0, compileSDK37), retaining targetSDK35/minSDK26. Prior source adapters are retained pending verification on the matching renderer.
