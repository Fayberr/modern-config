package net.fayber.modernconfig.api;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class EntryVisibilityTest {
    @Test public void combinesLiveConditionWithTab() {
        AtomicBoolean minimized = new AtomicBoolean(false);
        EntryVisibility visibility = new EntryVisibility(minimized::get);
        assertFalse(visibility.isVisible(1, 1));
        minimized.set(true);
        assertTrue(visibility.isVisible(1, 1));
        assertFalse(visibility.isVisible(1, 0));
        assertTrue(visibility.isVisible(-1, 0));
        minimized.set(false);
        assertFalse(visibility.isVisible(-1, 0));
    }
}
