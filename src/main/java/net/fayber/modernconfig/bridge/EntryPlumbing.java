package net.fayber.modernconfig.bridge;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

/**
 * Value plumbing of one Cloth entry, captured by FieldBuilderMixin while the builder was
 * building it. Cloth exposes the current value publicly but keeps the persisting consumer
 * private, and slider bounds live on the builder, so build time is the only chance to grab
 * these. ClothBridge reads them when it translates the screen.
 *
 * @param saveConsumer Cloth's own consumer, must run on Save only
 * @param min          lower bound for range entries, null otherwise
 * @param max          upper bound for range entries, null otherwise
 */
public record EntryPlumbing(@Nullable Consumer<Object> saveConsumer,
                            @Nullable Object min,
                            @Nullable Object max) {
}
