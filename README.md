<div align="center">

# CUITicket

**Report once. Reach every admin. From any client.**

A ticket system for Minecraft servers, built on [CrossUI](https://github.com/natan307/CrossUI).
Bedrock forms, Java dialogs and chest menus from one plugin.

[![License](https://img.shields.io/github/license/natan307/CUITicket?style=for-the-badge&color=blue)](LICENSE)
[![Release](https://img.shields.io/github/v/release/natan307/CUITicket?style=for-the-badge&color=green)](https://github.com/natan307/CUITicket/releases)

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.6%2B-62b47a?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Paper](https://img.shields.io/badge/Paper-supported-blue?style=flat-square)
![Spigot](https://img.shields.io/badge/Spigot-supported-yellow?style=flat-square)
![Purpur](https://img.shields.io/badge/Purpur-supported-purple?style=flat-square)
![Folia](https://img.shields.io/badge/Folia-supported-brightgreen?style=flat-square)

[![Wiki](https://img.shields.io/badge/Docs-Wiki-informational?style=for-the-badge&logo=gitbook&logoColor=white)](https://github.com/natan307/CUITicket/wiki)
[![CrossUI](https://img.shields.io/badge/Requires-CrossUI-5865F2?style=for-the-badge)](https://github.com/natan307/CrossUI)
[![Ko-fi](https://img.shields.io/badge/Support%20me-Ko--fi-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/natanezra)

</div>

---

## Features

- **One ticket flow for every client.** Bedrock players get forms, Java 1.21.6+ players get dialogs, older clients get a chest menu. CrossUI decides.
- **Four categories out of the box:** Player Report, Bug Report, Suggestion, Question. Add your own in `config.yml`.
- **Staff menus.** Filter by open, replied, closed, assigned to me. Reply, assign, close and open again.
- **Notifications.** Staff get a clickable message for new tickets. Players are told about replies, even if they were offline.
- **Anti spam.** Limit of open tickets per player and a cooldown.
- **SQLite or MySQL.** SQLite needs no setup.
- **Folia safe.** Works on Paper, Spigot, Purpur and Folia.
- **Every text is configurable.**
- **Developer API** to create and read tickets from other plugins.

## Requirements

- Paper, Spigot, Purpur or Folia, **1.21.6 or newer**
- Java 21
- [CrossUI](https://github.com/natan307/CrossUI) **2.0.1 or newer**
- Geyser and Floodgate for Bedrock players

## Installation

1. Put `CrossUI.jar` and `CUITicket-<version>.jar` in your `plugins` folder.
2. Start the server.
3. Give staff `cuiticket.admin`.
4. Players use `/ticket`.

More in the [wiki](https://github.com/natan307/CUITicket/wiki/Installation).

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/ticket` | `cuiticket.create` | Create a ticket |
| `/ticket mine` | `cuiticket.create` | Your tickets |
| `/ticket <id>` | `cuiticket.create` | Details of one of your tickets |
| `/ticket cancel <id>` | `cuiticket.create` | Cancel your open ticket |
| `/ticketadmin` | `cuiticket.admin` | Ticket menu |
| `/ticketadmin list`, `open`, `closed` | `cuiticket.admin` | Filtered lists |
| `/ticketadmin view <id>` | `cuiticket.admin` | Open one ticket |
| `/ticketadmin reply <id> [message]` | `cuiticket.reply` | Reply to a ticket |
| `/ticketadmin close <id> [note]` | `cuiticket.admin` | Close a ticket |
| `/ticketadmin assign <id> <admin\|none>` | `cuiticket.admin` | Assign a ticket |
| `/ticketadmin stats` | `cuiticket.admin` | Statistics |
| `/ticketadmin reload` | `cuiticket.admin` | Reload `config.yml` |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `cuiticket.create` | everyone | Create tickets and read your own |
| `cuiticket.admin` | op | Use `/ticketadmin` (includes notify and reply) |
| `cuiticket.reply` | op | Reply to tickets |
| `cuiticket.notify` | op | Get notifications and the join summary |
| `cuiticket.bypass` | op | Ignore the cooldown and the open ticket limit |

## Developer API

```java
if (CUITicket.isAvailable()) {
    TicketManager tickets = CUITicket.getTicketManager();
    tickets.create(player.getUniqueId(), player.getName(),
            "BUG_REPORT", null, "Plugin X error when clicking", null);
}
```

Add `depend: [CUITicket]` (or `softdepend`) to your `plugin.yml` and compile against the CUITicket jar.
Details in the [wiki](https://github.com/natan307/CUITicket/wiki/Developer-API).

## Building

```
./gradlew build
```

The jar is `build/libs/CUITicket-<version>.jar`. The CrossUI API is fetched from JitPack.

## Privacy

CUITicket uses [bStats](https://bstats.org) to count servers and send anonymous numbers (storage type and ticket counts). No player data is sent. Turn it off with `metrics.enabled: false`.

## Support the project

If CUITicket is useful for your server, you can support its development:

[![Ko-fi](https://img.shields.io/badge/Buy%20me%20a%20coffee-Ko--fi-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/natanezra)

Bugs and ideas: [open an issue](https://github.com/natan307/CUITicket/issues).

## License

[MIT](LICENSE)
