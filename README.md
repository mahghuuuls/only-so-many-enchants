# Only So Many Enchants

Enforces a single configurable limit on how many distinct enchantments any item or enchanted book can carry across loot, trading, enchanting, anvils, commands, mobs, and existing saves.

- **Mod ID:** `onlysomanyenchants`
- **Version:** `1.0.0`
- **Target:** Minecraft 1.12.2 (Forge, Cleanroom compatible)
- **Required Dependency:** `mixinbooter@[10.7,)` (bundled in Cleanroom; install [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixinbooter) `10.7` or newer on standard Forge; packaged testing used Cleanroom `0.7.14-alpha` with MixinBooter `12.1`)
- **Install Side:** Required on the server; optional on clients (`acceptableRemoteVersions = "*"`). Installing on the client displays the anvil cap explanation inside the anvil GUI; unmodded clients receive the same explanation as an action-bar message.
- **Player-Facing Page:** [MOD-PAGE.md](MOD-PAGE.md)
- **Release History:** [CHANGELOG.md](CHANGELOG.md)
- **License:** MIT (see [LICENSE](LICENSE) and [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md))

## Configuration

`config/onlysomanyenchants.cfg`

- `general.maxDistinctEnchantments` (integer, default `1`, minimum `1`): shared maximum number of distinct enchantments allowed on any item or enchantment-storing book/plate. Values below `1` log an error and fall back to `1`.
- `general.diagnosticMode` (boolean, default `false`): enables rate-limited server log lines at trimming, rejection, and migration boundaries for pack troubleshooting.

## Operator Command

- `/onlysomanyenchants inspect` (permission level `2`): read-only inspection of the executing player's main-hand item showing item ID, counted distinct enchantments and levels, active cap, and cap status.

## Scope Notes

- Counts every vanilla enchantment, modded enchantment, and curse equally (`1` distinct enchantment regardless of level) across `ench` and `StoredEnchantments` NBT.
- Does not alter enchantment levels, innate item abilities, durability, custom names, or unrelated NBT such as [Quality Tools](https://www.curseforge.com/minecraft/mc-mods/quality-tools) modifiers.
- Dedicated optional integrations ([Easy Magic (Backport)](https://www.curseforge.com/minecraft/mc-mods/easy-magic-backport), [Simple Storage Network](https://www.curseforge.com/minecraft/mc-mods/simple-storage-network), [Baubles](https://www.curseforge.com/minecraft/mc-mods/baubles), [Intrinsic Backpack](https://www.curseforge.com/minecraft/mc-mods/intrinsic-backpack), [Cosmetic Armor Reworked](https://www.curseforge.com/minecraft/mc-mods/cosmetic-armor-reworked)) are absence-safe and never required; standard Forge containers and capability-backed backpacks (such as [Iron Chests](https://www.curseforge.com/minecraft/mc-mods/iron-chests) and [Simply Backpacks](https://www.curseforge.com/minecraft/mc-mods/simply-backpacks)) migrate generically.
- Does not filter display-only recipe viewer pages (JEI/HEI) and does not provide an undo UI for removed enchantments.
