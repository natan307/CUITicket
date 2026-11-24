package io.github.natan307.cuiticket.storage;

import io.github.natan307.cuiticket.CUITicketPlugin;
import io.github.natan307.cuiticket.ticket.Ticket;
import io.github.natan307.cuiticket.ticket.TicketReply;
import io.github.natan307.cuiticket.ticket.TicketStatus;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * SQLite (default, a file in the plugin folder) or MySQL. Both JDBC drivers are bundled with
 * Spigot and Paper, so nothing extra has to be installed. Writes run on one background thread.
 */
public final class SqlStorage implements TicketStorage {

    private final CUITicketPlugin plugin;
    private final boolean mysql;
    private final String url;
    private final String user;
    private final String password;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "CUITicket-Storage");
        thread.setDaemon(true);
        return thread;
    });

    public SqlStorage(CUITicketPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("storage.type", "sqlite").toLowerCase(Locale.ROOT);
        this.mysql = type.equals("mysql");
        if (mysql) {
            String host = config.getString("storage.mysql.host", "localhost");
            int port = config.getInt("storage.mysql.port", 3306);
            String database = config.getString("storage.mysql.database", "crossui_ticket");
            this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&autoReconnect=true";
            this.user = config.getString("storage.mysql.username", "root");
            this.password = config.getString("storage.mysql.password", "");
        } else {
            File file = new File(plugin.getDataFolder(), "tickets.db");
            this.url = "jdbc:sqlite:" + file.getAbsolutePath();
            this.user = null;
            this.password = null;
        }
    }

    public String typeName() {
        return mysql ? "mysql" : "sqlite";
    }

    private Connection connect() throws SQLException {
        return mysql ? DriverManager.getConnection(url, user, password) : DriverManager.getConnection(url);
    }

    @Override
    public void init() throws Exception {
        plugin.getDataFolder().mkdirs();
        try {
            Class.forName(mysql ? "com.mysql.cj.jdbc.Driver" : "org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The " + typeName() + " JDBC driver is not available on this server.", e);
        }
        String suffix = mysql ? " DEFAULT CHARSET=utf8mb4" : "";
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS tickets ("
                    + "number INT NOT NULL PRIMARY KEY,"
                    + "player_id VARCHAR(36) NOT NULL,"
                    + "player_name VARCHAR(64) NOT NULL,"
                    + "category VARCHAR(64) NOT NULL,"
                    + "target VARCHAR(64),"
                    + "reason TEXT NOT NULL,"
                    + "evidence TEXT,"
                    + "status VARCHAR(16) NOT NULL,"
                    + "assigned_to VARCHAR(36),"
                    + "assigned_name VARCHAR(64),"
                    + "created_at BIGINT NOT NULL,"
                    + "updated_at BIGINT NOT NULL,"
                    + "player_unread INT NOT NULL"
                    + ")" + suffix);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ticket_replies ("
                    + "ticket_number INT NOT NULL,"
                    + "reply_index INT NOT NULL,"
                    + "author_id VARCHAR(36) NOT NULL,"
                    + "author_name VARCHAR(64) NOT NULL,"
                    + "message TEXT NOT NULL,"
                    + "created_at BIGINT NOT NULL,"
                    + "staff INT NOT NULL,"
                    + "PRIMARY KEY (ticket_number, reply_index)"
                    + ")" + suffix);
        }
    }

    @Override
    public List<Ticket> loadAll() throws Exception {
        Map<Integer, Ticket> tickets = new HashMap<>();
        List<Ticket> order = new ArrayList<>();
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("SELECT * FROM tickets ORDER BY number")) {
                while (rs.next()) {
                    String assigned = rs.getString("assigned_to");
                    Ticket ticket = new Ticket(
                            rs.getInt("number"),
                            UUID.fromString(rs.getString("player_id")),
                            rs.getString("player_name"),
                            rs.getString("category"),
                            rs.getString("target"),
                            rs.getString("reason"),
                            rs.getString("evidence"),
                            TicketStatus.parse(rs.getString("status"), TicketStatus.OPEN),
                            assigned == null || assigned.isEmpty() ? null : UUID.fromString(assigned),
                            rs.getString("assigned_name"),
                            rs.getLong("created_at"),
                            rs.getLong("updated_at"),
                            rs.getInt("player_unread") != 0);
                    tickets.put(ticket.number(), ticket);
                    order.add(ticket);
                }
            }
            try (ResultSet rs = statement.executeQuery("SELECT * FROM ticket_replies ORDER BY ticket_number, reply_index")) {
                while (rs.next()) {
                    Ticket ticket = tickets.get(rs.getInt("ticket_number"));
                    if (ticket == null) {
                        continue;
                    }
                    ticket.restoreReply(new TicketReply(UUID.fromString(rs.getString("author_id")),
                            rs.getString("author_name"), rs.getString("message"), rs.getLong("created_at"),
                            rs.getInt("staff") != 0));
                }
            }
        }
        return order;
    }

    @Override
    public void save(Ticket ticket) {
        writer.execute(() -> {
            try (Connection connection = connect()) {
                connection.setAutoCommit(false);
                try {
                    writeTicket(connection, ticket);
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save ticket " + ticket.id(), e);
            }
        });
    }

    private void writeTicket(Connection connection, Ticket ticket) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("REPLACE INTO tickets (number, player_id, player_name, "
                + "category, target, reason, evidence, status, assigned_to, assigned_name, created_at, updated_at, "
                + "player_unread) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, ticket.number());
            ps.setString(2, ticket.playerId().toString());
            ps.setString(3, ticket.playerName());
            ps.setString(4, ticket.categoryId());
            ps.setString(5, ticket.target());
            ps.setString(6, ticket.reason());
            ps.setString(7, ticket.evidence());
            ps.setString(8, ticket.status().name());
            UUID assigned = ticket.assignedTo();
            ps.setString(9, assigned == null ? null : assigned.toString());
            ps.setString(10, ticket.assignedName());
            ps.setLong(11, ticket.createdAt());
            ps.setLong(12, ticket.updatedAt());
            ps.setInt(13, ticket.playerUnread() ? 1 : 0);
            ps.executeUpdate();
        }
        List<TicketReply> replies = ticket.replies();
        try (PreparedStatement ps = connection.prepareStatement("REPLACE INTO ticket_replies (ticket_number, "
                + "reply_index, author_id, author_name, message, created_at, staff) VALUES (?,?,?,?,?,?,?)")) {
            for (int i = 0; i < replies.size(); i++) {
                TicketReply reply = replies.get(i);
                ps.setInt(1, ticket.number());
                ps.setInt(2, i);
                ps.setString(3, reply.authorId().toString());
                ps.setString(4, reply.authorName());
                ps.setString(5, reply.message());
                ps.setLong(6, reply.createdAt());
                ps.setInt(7, reply.staff() ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public void delete(int number) {
        writer.execute(() -> {
            try (Connection connection = connect()) {
                connection.setAutoCommit(false);
                try (PreparedStatement replies = connection.prepareStatement("DELETE FROM ticket_replies WHERE ticket_number = ?");
                     PreparedStatement ticket = connection.prepareStatement("DELETE FROM tickets WHERE number = ?")) {
                    replies.setInt(1, number);
                    replies.executeUpdate();
                    ticket.setInt(1, number);
                    ticket.executeUpdate();
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not delete ticket " + Ticket.idOf(number), e);
            }
        });
    }

    @Override
    public void close() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(15, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Some tickets could not be written before shutdown.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
