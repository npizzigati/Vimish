![vimish-logo-smallest](https://user-images.githubusercontent.com/54257961/202854465-e35f6d86-a77d-4397-b8be-4699b79e7fbb.png)

> Vim mode for the OmegaT computer-aided translation tool

### Install and run
Download the [latest release](https://github.com/npizzigati/Vimish/releases/tag/v0.1.1) and copy it to your OmegaT plugin directory.

The next time you start OmegaT, the translation editor pane will be in Vim mode.

To uninstall, simply remove the jar file from the plugin directory.

### Features
Vimish implements a large subset of Vim's features, including:
- Normal, insert, visual and replace modes.
- Character-wise, word-wise and text-object movement/deletion/changes.
- Yanking and putting using system clipboard or registers.
- Repeating searches, finds and changes.
- Key mappings.

It also implements key chords (mapping two simultaneous key presses to a Vim sequence). Among other things, this lets you map the key combination `j` `k` in insert mode to `<Esc>` (to switch to normal mode). [Here's how](#recipes).

### Settings


### Recipes
#### Use the key chord `j` `k` to exit insert mode
1. Go to the Vimish options menu: `Options -> Preferences -> Vimish` (`Vimish` is under the `Plugins` subheading in the left-hand pane).
2. In the `Key Chords` section, choose `Insert` from the `Mode` dropdown menu.
3. click on the `Add` button to the right.
4. Type `jk` under the `Keys` heading, and `<Esc>` under the `Mapped to` heading, like so:
5. Click on the `Ok` button at the bottom of the dialog to confirm.

### Build from source

Clone repository, then execute:

`./gradlew build` (Linux/Mac)

or

`gradlew` (Windows)

from the `Vimish` base directory.

The plugin jar file will be created in `Vimish/build/libs/`.
