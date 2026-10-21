package net.fayber.modernconfig.api;

import java.util.function.BooleanSupplier;

/** Combines an option's live display condition with the active tab. */
public record EntryVisibility(BooleanSupplier condition) {
    public boolean isVisible(int tab, int activeTab) {
        return (tab < 0 || tab == activeTab) && condition.getAsBoolean();
    }
}
