Items and enchanted books are limited to a single configurable number of distinct enchantments (default `1`). Excess enchantments are trimmed automatically before first gameplay preview, pickup, or craft receipt, while deliberate over-limit combinations at an anvil or via `/enchant` are blocked without consuming inputs.

<span style="color:#d6a100">**AI usage disclaimer:** This mod was developed with AI-agent assistance using [this agent workflow](https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow). The project owner reviewed the work during development.</span>

---

**Warning for existing worlds and cap changes:** Adding this mod to an existing world or lowering `maxDistinctEnchantments` permanently removes excess enchantments at random from over-limit items in inventories, chests, mob gear, and supported modded storage. Removed enchantments cannot be restored by raising the cap later. Back up your world before adding the mod or lowering the limit.

---

## How It Works

- **Single shared cap:** One setting (`maxDistinctEnchantments`, default `1`) applies to all enchantable gear, enchanted books, and enchantment-storing items. Every vanilla enchantment, modded enchantment, and curse counts as `1` distinct enchantment regardless of its level. Enchantment levels, innate item abilities, durability, custom names, and non-enchantment traits such as [Quality Tools](https://www.curseforge.com/minecraft/mc-mods/quality-tools) qualities are left untouched.
- **Automatic trimming before first view:** Over-limit items from loot chests, fishing, loose drops, villager trades, mob equipment, crafted outputs, Creative mode, and `/give` are reduced to the cap by equal-chance random selection before your first gameplay preview, pickup, or craft receipt. Surviving enchantments keep their original levels and persist across drops, container reopens, reconnects, and restarts, so villager trade previews match purchases and dropped mob gear matches what the mob used.
- **Enchanting table protection:** The enchanting table always grants the promised enchantment and level shown before you enchant. Any extra rolled enchantments beyond the cap are trimmed randomly.
- **Anvil and command cap rejection:** Deliberate combinations at an anvil or via `/enchant` that would exceed the cap are blocked without consuming inputs, materials, or XP. Legal upgrades that stay within the cap (such as upgrading an existing enchantment's level, repairing, or renaming) continue to work normally. When the cap alone blocks an anvil combination, the anvil shows `This would give the item <count> enchantments. The limit is <cap>.` and clears the notice as soon as the inputs change. Ordinary vanilla or modded incompatibilities keep their normal behavior with no cap notice.
- **Existing-world migration:** When added to an existing save or after lowering the cap, over-limit gear and books in player inventories, Shulker Boxes, chests, and mob gear are trimmed automatically on login, chunk load, or first container open without forcing unloaded chunks to load.

## Configuration And Command

Open `config/onlysomanyenchants.cfg` to adjust settings. Changes require a restart.

```ini
general {
    # Maximum distinct enchantments allowed on any item or enchanted book (minimum 1).
    I:maxDistinctEnchantments=1

    # Logs rate-limited trim, rejection, and migration events on the server when troubleshooting.
    B:diagnosticMode=false
}
```

**Operator inspection:** Operators (permission level `2`) can run `/onlysomanyenchants inspect` to check the held item's counted distinct enchantments, levels, active cap, and cap status without modifying the item.

## Mod Compatibility

- **[Easy Magic (Backport)](https://www.curseforge.com/minecraft/mc-mods/easy-magic-backport):** Trims displayed enchanting table hints and rerolled offers to the cap so the shown hint matches what you receive.
- **[Simple Storage Network](https://www.curseforge.com/minecraft/mc-mods/simple-storage-network):** Trims over-limit items inside connected unopened chests before the first network listing without breaking item counts on withdrawal.
- **Modded storage and carried slots:** Migrates standard Forge chests, containers, and capability-backed backpacks on chunk load or open (tested with [Iron Chests](https://www.curseforge.com/minecraft/mc-mods/iron-chests) and [Simply Backpacks](https://www.curseforge.com/minecraft/mc-mods/simply-backpacks)), and adds dedicated carried-slot migration for [Baubles](https://www.curseforge.com/minecraft/mc-mods/baubles), [Intrinsic Backpack](https://www.curseforge.com/minecraft/mc-mods/intrinsic-backpack), and [Cosmetic Armor Reworked](https://www.curseforge.com/minecraft/mc-mods/cosmetic-armor-reworked).
- **Anvil and curse mods:** Allows legal [Quark](https://www.curseforge.com/minecraft/mc-mods/quark) Ancient Tome level upgrades and [Charm](https://www.curseforge.com/minecraft/mc-mods/charm) CurseBreak curse removals while blocking over-cap combinations, and randomly trims items back down to the cap when a passive mid-game effect adds an enchantment or curse past the limit (such as Charm Spectres, where either the curse or an existing enchantment may survive).

## Installation

- **Dependency:** Requires [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixinbooter) `10.7` or newer on standard Forge (already included in Cleanroom; packaged testing used Cleanroom `0.7.14-alpha` with MixinBooter `12.1`).
- **Client and server:** Required on the server; optional on clients. In multiplayer, the server's cap always governs enforcement. Installing the mod on the client shows the cap explanation inside the anvil screen; clients without the mod still connect normally and see the same explanation on the action bar.
- **Source code:** [GitHub repository](https://github.com/mahghuuuls/only-so-many-enchants)
