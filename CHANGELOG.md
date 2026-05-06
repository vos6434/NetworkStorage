# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.4.0] - 2026-05-06

### Added

- Storage network terminal block item (`OAK_SHELF`-based) with custom model support and `/storage terminal` give command.
- Configurable shaped recipes for storage terminal, wireless terminal, and storage wand; plank ingredient accepts any wood planks (`PLANKS`).
- Recipe book discovery on join/reload for plugin recipes.
- Resource pack workflow for custom GUI, wand (`STICK`), wireless terminal (`COMPASS`), and terminal block inventory models (Minecraft 1.21.x pack format).
- Persists each player’s last terminal sort mode and search filter in `player-state.yml`.
- VS Code task + scripts to rebuild plugin and start Paper in a visible console window.

### Changed

- Wireless terminal uses `COMPASS` base item with unlimited use (durability removed).
- Storage wand uses `STICK` base item; wand stacks to max 1.
- Terminal feedback uses action bar (white); search prompts remain in chat.
- Custom plugin item tooltip titles use white; lore colors preserved.
- Breaking a linked storage terminal drops the custom terminal item instead of a vanilla shelf.
- Terminal GUI deposit/withdraw controls refined (left / right / shift combinations); shift-right on inventory deposits all matching stacks; wireless-open GUI blocks storing the wireless terminal used to open it.

### Fixed

- Vanilla compass appearance when resource pack overrides `compass.json`.

---

## Earlier versions

Prior iterations existed outside this changelog; treat **1.4.0** as the first fully documented release for this fork/workspace layout.
