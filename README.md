# NetworkStorage

A Minecraft plugin that allows players to create a centralized storage network.

This repository is a **fork** of [DerMoha/NetworkStorage](https://github.com/DerMoha/NetworkStorage), extended with a terminal block item, configurable recipes, a companion resource pack, and UX improvements. Upstream documentation below still applies unless noted in [Fork changes](#fork-changes-this-repository).

---

## Features

* Create a network of chests to store your items.
* Access all your items from a single terminal.
* Trust other players to access your network.
* Track player deposits and withdrawals
* Wireless access to your network with a wireless terminal.
* **Configurable Network Modes:** Choose between individual player networks or a single, server-wide global network.
* **Optional Trust System:** Make networks publicly accessible, perfect for cooperative servers.

## Commands

* `/storage wand`: Get the storage wand.
* `/storage info`: View your network information.
* `/storage trust <player>`: Trust a player to your network.
* `/storage untrust <player>`: Untrust a player from your network.
* `/storage wireless`: Get a wireless terminal.

## How to Use

1. Use `/storage wand` to get a storage wand.
2. Right-click with the wand to create a new network.
3. Left-click on chests with the wand to add them to your network.
4. Place a terminal to access all your items in one place.

---

## Important Notes

### Network Modes (`network-mode`)

In the `config.yml`, you can choose between two modes:

* **`PLAYER` (Default):** Each player has their own private network. This is the classic behavior.
* **`GLOBAL`:** There is only one, single, massive network for the entire server. All players share this one network. Commands like `/storage trust` or `/network` are disabled in this mode.

### Trust System (`enable-trust-system`)

This option is only relevant in `PLAYER` mode:

* **`true` (Default):** Only the owner and players they have added via `/storage trust` can access a network.
* **`false`:** The trust system is completely disabled. **Every player can access every network.** Ideal for small, private servers where everyone works together.

Trust checks apply to terminals and wireless access. Physical storage chests are still normal Minecraft chests, so use your server's land-claim or block-protection plugin if players should not open them directly.

### Safety Compatibility

* Storage wands and wireless terminals must be created by this plugin. Items that only match the display name are rejected intentionally, because renamed legacy items cannot be distinguished from forged items.
* New network names are limited to 1-32 characters: letters, numbers, spaces, underscores, hyphens, and apostrophes. This keeps names safe for YAML-backed storage.

### Language and Item Search

* **Display:** The names of items in the terminal are always displayed in the language you have set in your Minecraft client (e.g., German, French, etc.).
* **Search:** The search function works with the internal, English material names (e.g., `diamond`, `stone`, `iron_ingot`) or with custom names you have given an item using an anvil. This means you must search in English, even if the items are displayed in your own language.

---

## Fork changes (this repository)

The following builds on top of [DerMoha/NetworkStorage](https://github.com/DerMoha/NetworkStorage). See [CHANGELOG.md](CHANGELOG.md) for full detail.

### Gameplay and items

* **`/storage terminal`** — gives the craftable **storage network terminal** block item (default base: `OAK_SHELF` with custom model data). Placing it **auto-links** to the placer’s network (permission `networkstorage.terminal`).
* **Configurable shaped recipes** for the terminal, wireless terminal, and wand in `config.yml`; **`PLANKS`** matches any wood plank type.
* **Recipe book discovery** for plugin recipes when players join / after reload.
* **Wireless terminal** uses a **`COMPASS`** base item with unlimited use (old durability limits removed).
* **Storage wand** uses a **`STICK`** base item and **max stack size 1** for the plugin wand.
* **Breaking** a linked storage terminal drops the **plugin terminal item**, not a plain vanilla shelf.
* **Terminal GUI**: richer deposit/withdraw controls (left/right/shift-click patterns); opening via wireless blocks depositing *that* wireless terminal into the network; sort mode and search filter **persist per player** in `player-state.yml`.
* **Messaging**: most terminal actions use the **action bar** (white text); search flow keeps **chat** where appropriate.
* **Tooltips**: plugin-introduced items use **white** display titles; lore colors preserved.

### Resource pack and tooling

* Optional **resource pack** sources under `resourcepacks/networkstorage-test-gui/` (GUI font, terminal controls, wand/wireless/terminal models; Minecraft **1.21.x** pack format). Scripts help build/copy without overwriting your custom PNGs when regenerating.
* PowerShell **build / install / dev-server / resource-pack** scripts at repo root; VS Code task for rebuild + **visible** Paper console.

### Repository layout (monorepo)

| Path | Contents |
|------|-----------|
| `NetworkStorage/` | Maven Paper plugin (`pom.xml`, `src/`) |
| `resourcepacks/networkstorage-test-gui/` | Resource pack sources |
| `*.ps1` | Build and development helpers |

From the repo root (PowerShell):

```powershell
.\build-plugin.ps1
.\install-plugin.ps1
```

`dev-server/` and `tools/` are local-only (ignored by Git). Plugin runtime config and `player-state.yml` live under `plugins/NetworkStorage/` on your server.
