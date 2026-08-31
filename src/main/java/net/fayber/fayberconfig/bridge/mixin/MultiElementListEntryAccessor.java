package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * A MultiElementListEntry is an expandable group of entries built directly rather than through
 * a builder, so its children live in this private field with no public read path. The field's
 * erased type is List, so the accessor return type must match it exactly or the mixin fails
 * to apply.
 */
@Mixin(MultiElementListEntry.class)
public interface MultiElementListEntryAccessor {
    @Accessor("entries")
    List<AbstractConfigListEntry<?>> fayberconfig$entries();
}
