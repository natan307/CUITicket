package io.github.natan307.cuiticket.ticket;

/**
 * A kind of ticket, configured in {@code tickets.categories}.
 *
 * @param id             unique id such as {@code BUG_REPORT}
 * @param label          name shown to players
 * @param icon           item name for the button picture (for example {@code IRON_SWORD}), or empty for none
 * @param requiresTarget whether the player must name another player
 */
public record TicketCategory(String id, String label, String icon, boolean requiresTarget) {
}
