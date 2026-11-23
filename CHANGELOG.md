# Changelog

All notable changes to Only So Many Enchants are documented in this file.

## [1.0.0] - 2026-11-23

### Added
- Single configurable `maxDistinctEnchantments` cap (default `1`, minimum `1`) in `config/onlysomanyenchants.cfg`, counting vanilla enchantments, modded enchantments, and curses equally across `ench` and `StoredEnchantments` while leaving enchantment levels, innate item abilities, durability, custom names, and unrelated NBT untouched.
- Persistent equal-chance automatic trimming before first gameplay view or pickup across loot chests, fishing, loose drops, villager trades, mob equipment (spawn, load, and mid-life swaps), crafting outputs, Creative inventory actions, `/give`, and passive mid-game enchantment additions.
- Enchanting table enforcement that always grants the displayed promised enchantment and level while trimming excess rolled bonuses.
- Non-destructive anvil and `/enchant` cap rejection that preserves input items and XP while allowing legal upgrades within the cap.
- Cap-only anvil rejection explanation rendered inside the anvil GUI on modded clients and sent as a rate-limited action-bar message to unmodded clients.
- Automatic existing-world and lowered-cap migration across player inventories, chests, Shulker Boxes, and supported modded storage and carried slots without forcing chunk loads.
- Disabled-by-default `diagnosticMode` server logging and read-only `/onlysomanyenchants inspect` operator command.

### Compatibility
- Requires `mixinbooter@[10.7,)` (included in Cleanroom; install MixinBooter `10.7` or newer on standard Forge; packaged testing used Cleanroom `0.7.14-alpha` with MixinBooter `12.1`). Server-authoritative with `acceptableRemoteVersions = "*"`.
- Dedicated absence-safe optional integrations for Easy Magic (Backport), Simple Storage Network, Baubles, Intrinsic Backpack, and Cosmetic Armor Reworked; generic Forge container/backpack migration verified with Iron Chests and Simply Backpacks; coexistence verified alongside Quark Ancient Tomes, Charm (CurseBreak and Spectre curses), PiTweaks, Immortal Anvils, Enchantment Control, Quality Tools, Rad Enchants, Disenchanter, BiblioCraft Reshelved, Defiled Lands, and Rough Mobs Revamped.
