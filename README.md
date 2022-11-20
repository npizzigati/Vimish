![vimish-logo-smallest](https://user-images.githubusercontent.com/54257961/202854465-e35f6d86-a77d-4397-b8be-4699b79e7fbb.png)

> Vim mode for the OmegaT computer-aided translation tool

### Install and run
Download the jar file from the [latest release](https://github.com/npizzigati/Vimish/releases/latest) and copy it to your OmegaT plugin directory.

The next time you start OmegaT, the translation editor pane will be in Vim mode.

To uninstall, simply remove the jar file from the plugin directory. When you restart OmegaT, your editing window will no longer be in Vim mode.

### Features
Vimish implements a large subset of Vim's features, including:
- Normal, insert, visual and replace modes.
- Character-wise, word-wise and text-object movement/deletion/changes.
- Yanking and putting using the system clipboard or registers.
- Repeating searches, finds and changes.
- Key mappings.

It also implements key chords (mapping two simultaneous key presses to a Vim sequence). Among other things, this lets you map the key combination `j` `k` in insert mode to `<Esc>` (to switch to normal mode). [Here's how](#recipes).

### Settings
General settings, key mappings and key chords can be viewed and changed in the Vimish options menu, `Options -> Preferences -> Plugins -> Vimish` (`Vimish` is under the `Plugins` subheading in the left-hand pane).
![Screenshot from 2022-11-19 15-49-32](https://user-images.githubusercontent.com/54257961/202873015-c68e3637-8683-4c3f-9146-7e83e84b78a0.png)

Key maps are equivalent to Vim's noremap mappings (they do not map recursively). Key combos and their respective mappings can be of any length. Any word or punctuation character, as well as the special keys `<Esc>`, `<CR>`, `<BS>`, `<Del>`, `<Tab>`, `<S-Tab>`, `<Left>`, `<Right>`, `<Up>` and `<Down>`, can be used as part of the key combo or its mapping. 

Key chords are two-key combinations that can map to key sequences of any length. Key chord combos cannot contain special characters, but the mapping they trigger can. For example, we can map the key chord `jk` to `<Esc>`. 

The keys in key chord combos must be pressed simultaneously for their mappings to trigger.

### Recipes
#### Use the key chord `j` `k` to exit insert mode
1. Go to the Vimish options menu: `Options -> Preferences -> Plugins -> Vimish` ([Help me find it](#Settings)).
2. In the `Key Chords` section, choose `Insert` from the `Mode` dropdown menu.
3. Click on the `Add` button to the right.
4. Type `jk` under the `Keys` heading, and `<Esc>` under the `Mapped to` heading, like so:
![Screenshot from 2022-11-19 15-42-00](https://user-images.githubusercontent.com/54257961/202872736-9007c772-1c7a-4f53-89b5-4f8b6840fc4d.png)
5. Click on the `Ok` button at the bottom of the dialog to confirm.

#### Search and replace (words)
1. In the editor window, type `/` or `?` to begin a forward/backward search, enter the word to search for (or a unique part of it) and press return to go to the word.
2. Type `ciw` (mnemonic: **c**hange **i**n **w**ord), type your replacement word, and hit the `<Esc>` key to return to normal mode.
3. Repeat the search by pressing the `n` key (or `N` to repeat the search in the opposite direction).
4. Hit the period/dot key (`.`) to repeat the last change.
5. Repeat steps 3 and 4 until done.

### Limitations
Visual line movements (like moving up and down with the arrow keys in OmegaT or Vim's `gj` and `gk` movements) are not available. But, just as you might do with a long wrapped line in Vim, in Vimish you can navigate quickly inside a segment using word-wise/left-right motions and search/find.

### Development 
#### Building from source
Clone repository, then execute:

`./gradlew build` (Linux/Mac)

or

`gradlew build` (Windows)

from the `Vimish` base directory.

The plugin jar file will be created in `Vimish/build/libs/` (or `Vimish\build\libs\` on Windows).
