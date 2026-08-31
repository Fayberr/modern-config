package net.fayber.fayberconfig.bridge;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

/**
 * The value plumbing of one Cloth config entry, captured while its builder was building it.
 *
 * <p>Cloth entries expose their current value publicly ({@code ValueHolder.getValue()}) but keep
 * the consumer that persists it private, and a slider's bounds live on the builder rather than on
 * the entry. Both are only reachable at build time, so
 * {@link net.fayber.fayberconfig.bridge.mixin.FieldBuilderMixin} records them here, keyed by the
 * entry instance, for {@link ClothBridge} to use when it translates the screen.
 *
 * @param saveConsumer Cloth's own consumer; it persists the value and must run on Save only.
 * @param min          lower bound for range entries, null when the entry is not ranged
 * @param max          upper bound for range entries, null when the entry is not ranged
 */
public record EntryPlumbing(@Nullable Consumer<Object> saveConsumer,
                            @Nullable Object min,
                            @Nullable Object max) {
}
