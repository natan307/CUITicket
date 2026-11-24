package io.github.natan307.cuiticket.storage;

import io.github.natan307.cuiticket.ticket.Ticket;

import java.util.List;

/** Where tickets are kept between restarts. */
public interface TicketStorage {

    /** Opens the storage and creates the tables. Called once, before {@link #loadAll()}. */
    void init() throws Exception;

    /** Reads every ticket, with its replies. */
    List<Ticket> loadAll() throws Exception;

    /** Writes a ticket and its replies in the background. */
    void save(Ticket ticket);

    /** Removes a ticket and its replies in the background. */
    void delete(int number);

    /** Waits for pending writes and closes the storage. */
    void close();
}
