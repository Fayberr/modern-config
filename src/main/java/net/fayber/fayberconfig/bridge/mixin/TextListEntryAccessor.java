package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the description text of a Cloth static text entry. The entry's field name is useless
 * as a label: {@code ConfigEntryBuilderImpl.startTextDescription} mints a random UUID for every
 * description (Cloth never displays the name), so the real text only lives in this private
 * field. Without the accessor the bridge would render UUIDs as section headers.
 */
@Mixin(TextListEntry.class)
public interface TextListEntryAccessor {
    @Accessor("text")
    Component fayberconfig$text();
}
