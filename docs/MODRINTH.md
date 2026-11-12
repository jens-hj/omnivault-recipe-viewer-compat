# OmniVault Recipe Viewer Compat

Makes [OmniVault](https://modrinth.com/mod/omnivault) terminals work properly with recipe viewers.

## With [REI](https://modrinth.com/mod/rei)

- **Move recipes into the crafting terminal.** The "+" button fills the crafting grid straight from your storage network and inventory, instead of "Move Items is not supported with this recipe and this container". Works with the wireless terminal too.
- **Load recipes into the Pattern Encoding Terminal.** Crafting recipes keep their shape; furnace and other machine recipes become processing patterns.
- **Look up items from the network grid.** Hover an item or fluid in a terminal and press your recipe or usage key.
- OmniVault terminals show up as crafting workstations, and REI's side panels no longer cover the terminal's sort buttons.

## With [JEI](https://modrinth.com/mod/jei)

OmniVault already moves recipes into its terminals with JEI. This mod adds what is missing:

- **Look up items from the network grid** with your recipe or usage key.
- JEI's bookmarks stay clear of the terminal's sort buttons.

## EMI

EMI has not been released for Minecraft 26.x yet. Support will be added to this mod when it is.

## Requirements

- [OmniVault](https://modrinth.com/mod/omnivault) 2.0.4 or newer
- REI or JEI (either, or neither; the mod just does nothing without one)
- Fabric or NeoForge, Minecraft 26.3

Client-side only: no need to install it on the server.
