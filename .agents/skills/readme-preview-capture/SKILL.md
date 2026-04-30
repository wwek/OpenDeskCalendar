---
name: readme-preview-capture
description: Use this project skill when updating OpenDeskCalendar README preview screenshots or regenerating docs/assets/readme-showcase.png. It captures light, dark, mono, and e-ink themes in portrait and landscape from an Android emulator, builds the README collage, and verifies the image assets.
---

# README Preview Capture

Use this skill when the user asks to refresh README screenshots, update the preview image, regenerate theme previews, or document the visual state of OpenDeskCalendar.

## Preconditions

- Run from the repository root.
- A single Android emulator/device is connected through `adb`.
- ImageMagick `magick` is installed.
- Use a debug build for capture. The script writes app preferences through `run-as`, which does not work with a non-debuggable release APK.

## Standard Workflow

1. Install a debuggable build:

   ```sh
   ./gradlew installDebug
   ```

2. Generate all screenshots and the README collage:

   ```sh
   ./.agents/skills/readme-preview-capture/scripts/generate-readme-preview.sh
   ```

3. Review the generated collage:

   ```sh
   open docs/assets/readme-showcase.png
   ```

4. Verify dimensions and git state:

   ```sh
   magick identify docs/assets/readme-showcase.png docs/assets/screenshots/*.png
   git diff --check
   git status --short
   ```

5. If README does not already reference the collage, add this block near the top:

   ```html
   <p align="center">
     <img src="docs/assets/readme-showcase.png" alt="OpenDeskCalendar 主题与横竖屏截图拼图">
   </p>
   ```

## Output Contract

The script writes:

- `docs/assets/readme-showcase.png`
- `docs/assets/screenshots/light-portrait.png`
- `docs/assets/screenshots/light-landscape.png`
- `docs/assets/screenshots/dark-portrait.png`
- `docs/assets/screenshots/dark-landscape.png`
- `docs/assets/screenshots/mono-portrait.png`
- `docs/assets/screenshots/mono-landscape.png`
- `docs/assets/screenshots/eink-portrait.png`
- `docs/assets/screenshots/eink-landscape.png`

Expected dimensions:

- Portrait screenshots: `1080x2400`
- Landscape screenshots: `2400x1080`
- Collage: generated from current screenshots, usually `1618x2152`

## Notes

- The script overwrites only screenshot assets and a minimal app preference XML on the emulator.
- It locks emulator rotation during capture and restores the app to dark landscape at the end.
- If capture shows a splash/blank screen, rerun the script after confirming the app starts normally; it waits for image byte size to exceed the ready threshold before accepting each screenshot.
