package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Cloth's startTextDescription gives every static text entry a random UUID as name and never
 * shows it, so the real description text only lives in this private field. Without this the
 * bridge would render UUIDs as section headers.
 */
@Mixin(TextListEntry.class)
public interface TextListEntryAccessor {
    @Accessor("text")
    Component fayberconfig$text();
}
