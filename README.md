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
General settings, key mappings and key chords can be viewed and changed in the Vimish options menu, `Options -> Preferences -> Vimish` (`Vimish` is under the `Plugins` subheading in the left-hand pane).
![Screenshot from 2022-11-19 15-49-32](https://user-images.githubusercontent.com/54257961/202873015-c68e3637-8683-4c3f-9146-7e83e84b78a0.png)

Key maps are equivalent to Vim's noremap mappings (they do not map recursively). Key combos and their respective mappings can be of any length. Any word or punctuation character, as well as the special keys `<Esc>`, `<CR>`, `<BS>`, `<Del>`, `<Tab>`, `<S-Tab>`, `<Left>`, `<Right>`, `<Up>` and `<Down>`, can be used as part of the key combo or its mapping. 

Key chords are two-key combinations that can map to key sequences of any length. Key chord combos cannot contain special characters, but the mapping they trigger can. For example, we can map the key chord `jk` to `<Esc>`. 

The keys in key chord combos must be pressed simultaneously for their mappings to trigger.

### Recipes
#### Use the key chord `j` `k` to exit insert mode
1. Go to the Vimish options menu: `Options -> Preferences -> Vimish` ([Help me find it](#Settings).
2. In the `Key Chords` section, choose `Insert` from the `Mode` dropdown menu.
3. click on the `Add` button to the right.
4. Type `jk` under the `Keys` heading, and `<Esc>` under the `Mapped to` heading, like so:
![Screenshot from 2022-11-19 15-42-00](https://user-images.githubusercontent.com/54257961/202872736-9007c772-1c7a-4f53-89b5-4f8b6840fc4d.png)

5. Click on the `Ok` button at the bottom of the dialog to confirm.

### Limitations
Since OmegaT segments have no newlines, line-wise movements (like moving up and down with the arrow keys or j and k) are not available. Just as you would with a long wrapped line in Vim, in Vimish you can navigate quickly inside a segment using word-wise/left-right motions and search/find.

### Development 
#### Building from source
Clone repository, then execute:

`./gradlew build` (Linux/Mac)

or

`gradlew build` (Windows)

from the `Vimish` base directory.

The plugin jar file will be created in `Vimish/build/libs/`.
