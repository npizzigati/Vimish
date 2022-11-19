![vimish-logo-smallest](https://user-images.githubusercontent.com/54257961/202854465-e35f6d86-a77d-4397-b8be-4699b79e7fbb.png)

> Vim-style modal editing for the OmegaT translation tool

### Installation and running

Download the [Vimish jar file](https://github.com/npizzigati/Vimish/releases/download/v0.1/Vimish-0.1.jar) and copy it to your OmegaT plugin directory.

The next time you start OmegaT, the translation editor will be in Vim mode.

To uninstall, simply remove the jar file from the plugin directory.

### Building from source

Clone repo, then execute:

`./gradlew build` (Linux/Mac)

or

`gradlew` (Windows)

from the base directory `Vimish`.

The plugin jar file will be created in the `Vimish/build/libs/` directory.

Copy this file to your OmegaT plugin directory.
