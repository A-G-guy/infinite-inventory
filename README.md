# Infinite Inventory

[简体中文](https://github.com/A-G-guy/infinite-inventory/blob/main/README.zh-CN.md)

![Infinite Inventory Logo](https://raw.githubusercontent.com/A-G-guy/infinite-inventory/main/.github/assets/infiniteinventory-logo.png)

[![GitHub Release](https://img.shields.io/github/v/release/A-G-guy/infinite-inventory?style=flat-square)](https://github.com/A-G-guy/infinite-inventory/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square&logo=minecraft)](https://minecraft.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange?style=flat-square&logo=java)](https://neoforged.net)
[![License](https://img.shields.io/github/license/A-G-guy/infinite-inventory?style=flat-square)](https://github.com/A-G-guy/infinite-inventory/blob/main/LICENSE)
[![Modrinth](https://img.shields.io/badge/Modrinth-Under_Review-1DBF7A?style=flat-square&logo=modrinth)](https://modrinth.com/mod/agguy-infinite-inventory)

**Never run out of inventory space again.**

Infinite Inventory is a NeoForge mod for Minecraft 1.21.1 that transforms the cramped player backpack into a **personal, infinitely-expandable, database-style storage system**. Instead of juggling chests, shulker boxes, and scattered storage rooms, you carry your entire item collection in a single searchable terminal — organized by tabs, filterable by bilingual search, and backed by automatic safety systems.

![Hero Banner](https://raw.githubusercontent.com/A-G-guy/infinite-inventory/main/.github/assets/infiniteinventory-hero-v2.png)

## Downloads

- **[Modrinth](https://modrinth.com/mod/agguy-infinite-inventory)** — Under review. Available once approved.
- **[GitHub Releases](https://github.com/A-G-guy/infinite-inventory/releases)** — All stable versions and changelogs.

## Why Download This Mod?

If you have ever:
- Run out of hotbar and inventory slots while mining or building
- Spent more time sorting chests than actually playing
- Lost track of which chest contains which item
- Wanted to find an item instantly without memorizing storage layouts

Then Infinite Inventory replaces all of that overhead with one full-screen database interface. Search by name, pinyin, item ID, tag, or mod — and extract exactly what you need in seconds.

## Core Features

- **Infinite Personal Storage** — One database, zero slot limits. Store as many unique item stacks as you want.
- **Full-Screen Database UI** — Multi-panel views: personal storage, public storage (server-wide shared pool), or mixed mode.
- **Bilingual Smart Search** — Understands Chinese display names, English aliases, item IDs, namespaces, and pinyin (full spelling + initials).
- **JEI-Style Advanced Search Syntax** — Filter by mod (`@`), item tag (`#`), registry name (`&`), creative tab (`%`), with AND/OR/NOT logic.
- **Tab Organization** — Create, rename, reorder, and assign custom icons to tabs. Move entire tab contents in one action.
- **Stable Sorting** — Sort by recent changes, recently added, name, count, namespace, or item ID.
- **Star & Note System** — Mark frequently-used items as starred, or attach custom notes to any item.
- **Operation Log Viewer** — Review history of deposits, extractions, and other database actions.
- **Auto-Store Enhancement** — Automatically send picked-up items into a designated target tab.
- **Data Safety** — Rolling auto-backups every 15 minutes, manual backup/restore commands, and unresolved-item preservation when mods are temporarily missing.
- **Optional Accessories Integration** — Wear the database terminal as a back-slot accessory for quick access.

## Environment

| Aspect | Value |
|---|---|
| **Side** | Client and Server (required on both for full functionality; server-side handles data persistence and public storage) |
| **Minecraft** | 1.21.1 |
| **Mod Loader** | NeoForge |
| **Supported NeoForge** | 21.1.222+ |
| **Java** | 21 |
| **Optional Dependency** | Accessories 1.1.0-beta.53+1.21.1 |

## Installation

1. Install Minecraft **1.21.1**.
2. Install a compatible **NeoForge** build in the **21.1.x** range.
3. Place the `Infinite Inventory` JAR into your `mods/` folder.
4. *(Optional)* Install **Accessories** if you want wearable terminal access from the back slot.

> **Note:** The mod must be present on **both client and server** for multiplayer. The server handles data persistence and public storage; the client renders the UI and handles search input.

## Crafting

Craft the **Database Terminal** in survival mode:

![Database Terminal](https://raw.githubusercontent.com/A-G-guy/infinite-inventory/main/.github/assets/database_access_item.png)

```text
D E D
R C R
D G D
```

| Symbol | Item |
|---|---|
| D | Diamond Block |
| E | Ender Pearl |
| R | Redstone Block |
| C | Chest |
| G | Gold Block |

Item ID: `infiniteinventory:database_access_item`

## How to Use

### Opening the Database
Right-click with the Database Terminal to open the full-screen interface.

### Switching Views
Use the top toolbar to toggle between:
- **Personal** — your own private item database
- **Public** — server-wide shared storage (if enabled)
- **Mixed** — combined view of both

### Searching
Type in the search bar to filter items. The search understands:
- Chinese item names and pinyin
- English display names and aliases
- Exact item IDs and namespaces

**Special search syntax:**

| Prefix | Meaning | Example |
|---|---|---|
| `@` | Filter by mod ID | `@infiniteinventory` |
| `#` | Filter by item tag | `#minecraft:logs` |
| `&` | Filter by registry name | `&diamond` |
| `%` | Filter by creative tab | `%building blocks` |
| `\|` | OR operator | `diamond \| emerald` |
| `-` | Exclude | `-stone` |

Spaces between terms act as **AND**.

### Extracting Items
- **Left-click** an item to extract 1
- **Shift+click** to extract a full stack
- **Right-click** for contextual options (custom amount, half stack)

### Managing Tabs
- Create tabs to categorize your items
- Drag tabs to reorder them
- Right-click a tab to rename, change icon, or move its entire contents
- Hide less-used tabs to keep the interface clean

### Auto-Store
Enable auto-store to automatically deposit newly picked-up items into a target tab of your choice.

## Data Safety

Your data is stored in the overworld's `SavedData` system, not on the terminal item itself. This means:

- **Terminal is replaceable** — losing or breaking the terminal does not delete your database.
- **Unresolved items preserved** — if a mod is temporarily removed, its items are kept as unresolved entries and restored when the mod returns.
- **Auto-backups** — the database is backed up automatically every 15 minutes.
- **Manual backup/restore** — server operators can create snapshots and restore from them at any time.

### Backup Commands

```
/infiniteinventory database backup now       # Create a manual backup
/infiniteinventory database backup list      # List available backups
/infiniteinventory database restore <name>   # Restore from a named backup
```

## Reference Documentation

- [Architecture Overview](https://github.com/A-G-guy/infinite-inventory/blob/main/docs/long-term/architecture/personal-database-architecture.md)
- [Safety & Recovery Guide](https://github.com/A-G-guy/infinite-inventory/blob/main/docs/long-term/operations/database-safety-and-recovery.md)
- [Public Release Checklist](https://github.com/A-G-guy/infinite-inventory/blob/main/docs/long-term/operations/public-release-checklist.md)

## Links

- **Source Code:** <https://github.com/A-G-guy/infinite-inventory>
- **Issue Tracker:** <https://github.com/A-G-guy/infinite-inventory/issues>
- **Modrinth:** <https://modrinth.com/mod/agguy-infinite-inventory>

## License

- Project license: [Apache-2.0](https://github.com/A-G-guy/infinite-inventory/blob/main/LICENSE)
- Third-party attributions: [THIRD_PARTY_NOTICES.md](https://github.com/A-G-guy/infinite-inventory/blob/main/THIRD_PARTY_NOTICES.md)
