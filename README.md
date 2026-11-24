# CUITicket

> Report once. Reach every admin. From any client.

A ticket system for Minecraft servers, part of the [CrossUI](https://github.com/natan307/CrossUI) ecosystem.
Players open tickets with a short form (Bedrock) or dialog (Java 1.21.6+), staff manage them in menus,
and players are told when staff replies, even if they were offline.

- Works on Paper, Spigot, Purpur and Folia, 1.21.6 and newer.
- Needs [CrossUI](https://github.com/natan307/CrossUI) **2.0.1** or newer.
- SQLite by default (nothing to set up), MySQL optional.

## What players can do

- `/ticket` opens a category menu: Player Report, Bug Report, Suggestion, Question (configurable).
- A short form asks for the player name (only for player reports), what happened, and an optional link with proof.
- `/ticket mine` shows their tickets and the replies.
- `/ticket cancel <id>` cancels a ticket that nobody has answered yet.
- A message with a button tells them when staff replies or closes the ticket. Offline players get it when they join.

## What staff can do

- `/ticketadmin` opens the ticket menu: open, replied, closed, all, assigned to me, statistics.
- Reply, assign to a staff member, close with an optional note, or open a closed ticket again.
- Staff with `cuiticket.notify` get a clickable message when a ticket is created, and a summary of waiting tickets when they join.
- The console gets the same information as text (`/ticketadmin list`, `reply <id> <message>`, and so on).

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/ticket` | `cuiticket.create` | Create a ticket |
| `/ticket mine` | `cuiticket.create` | Your tickets |
| `/ticket <id>` | `cuiticket.create` | Details of one of your tickets |
| `/ticket cancel <id>` | `cuiticket.create` | Cancel your ticket while it is still open |
| `/ticket help` | | Help and links |
| `/ticketadmin` | `cuiticket.admin` | Ticket menu |
| `/ticketadmin list`, `open`, `closed` | `cuiticket.admin` | Filtered lists |
| `/ticketadmin view <id>` | `cuiticket.admin` | Open one ticket |
| `/ticketadmin reply <id> [message]` | `cuiticket.reply` | Reply (opens a form when no message is given) |
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

## Configuration

Everything is in `config.yml`: categories (with item icons for Bedrock), cooldown, the limit of open
tickets, notifications, storage, and every text the plugin shows. Run `/ticketadmin reload` after editing.
Changing the storage type needs a restart.

## For developers

Other plugins can create and read tickets:

```java
TicketManager tickets = CUITicket.getTicketManager();
tickets.create(player.getUniqueId(), player.getName(), "BUG_REPORT", null, "Plugin X error when clicking", null);
List<Ticket> open = tickets.findByStatus(TicketStatus.OPEN);
```

Add `depend: [CUITicket]` (or `softdepend`) to your `plugin.yml` and compile against the CUITicket jar.

## Building

```
./gradlew build
```

The jar is `build/libs/CUITicket-<version>.jar`. The CrossUI API is fetched from JitPack.

## Privacy

CUITicket uses [bStats](https://bstats.org) to count servers and send anonymous numbers (storage type and
ticket counts). No player data is sent. Turn it off with `metrics.enabled: false`.

## License

MIT
