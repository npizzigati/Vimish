![Screenshot from 2022-11-19 05-15-13](https://user-images.githubusercontent.com/54257961/202847974-e528c541-736c-4722-a16a-274cea40f380.png)

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
