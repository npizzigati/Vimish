![Screenshot from 2022-11-18 23-07-20](https://user-images.githubusercontent.com/54257961/202835312-9095986f-c5f2-4588-ae34-51b9ff2f5a7d.png)

### Vim-style modal editing for the OmegaT translation tool

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
