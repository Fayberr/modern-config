package net.fayber.fayberconfig.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The custom GUI render pipeline behind every rounded shape in Fayber Config.
 *
 * <p>Vanilla's GUI can only fill axis-aligned rectangles, which is why "rounded" corners built out
 * of stacked fill bands look like staircases. This pipeline is vanilla's textured GUI pipeline
 * with one thing swapped: a fragment shader that computes circular coverage from the UVs and
 * anti-aliases it with {@code fwidth}, so corners are smooth at any GUI scale and any radius.
 *
 * <p>Everything except the fragment shader is copied off {@link RenderPipelines#GUI_TEXTURED} at
 * runtime rather than hard-coded, so blend mode, depth state, vertex format and uniform layout
 * automatically track whatever the current Minecraft version does. Blaze3D compiles pipelines
 * lazily on first use ({@code GlDevice#getOrCompilePipeline}), so no registration is needed; the
 * shader source is loaded from this mod's assets by the normal resource manager.
 *
 * <p>Construction is lazy and failure-tolerant: if anything about the vanilla pipeline changes in a
 * way this cannot mirror, {@link #roundCorner()} returns null and {@link Ui} falls back to drawing
 * corners as pixel spans. A config screen that looks slightly harder is better than a crash.
 */
public final class FayberPipelines {
    private static final Logger LOGGER = LoggerFactory.getLogger("FayberConfig");

    /** Flat white 4x4 texture: the shader samples it so the declared sampler is never optimised out. */
    public static final Identifier WHITE = Identifier.fromNamespaceAndPath("fayberconfig", "textures/gui/white.png");

    @Nullable
    private static RenderPipeline roundCorner;
    private static boolean built;

    private FayberPipelines() {
    }

    /** The anti-aliased corner pipeline, or null if it could not be assembled on this version. */
    @Nullable
    public static RenderPipeline roundCorner() {
        if (!built) {
            built = true;
            try {
                roundCorner = buildRoundCorner();
            } catch (Throwable t) {
                LOGGER.error("Could not build the rounded-corner pipeline; falling back to plain corners", t);
                roundCorner = null;
            }
        }
        return roundCorner;
    }

    private static RenderPipeline buildRoundCorner() {
        RenderPipeline base = RenderPipelines.GUI_TEXTURED;
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("fayberconfig", "pipeline/round_corner"))
                .withVertexShader(base.getVertexShader())
                .withFragmentShader(Identifier.fromNamespaceAndPath("fayberconfig", "core/round_corner"))
                .withVertexFormat(base.getVertexFormat(), base.getVertexFormatMode())
                .withPolygonMode(base.getPolygonMode())
                .withCull(base.isCull());

        // These are Optionals on the vanilla pipeline: GUI_TEXTURED leaves them unset, and the
        // builder setters reject null, so they are only copied when actually present.
        if (base.getColorTargetState() != null) {
            builder.withColorTargetState(base.getColorTargetState());
        }
        if (base.getDepthStencilState() != null) {
            builder.withDepthStencilState(base.getDepthStencilState());
        }

        for (String sampler : base.getSamplers()) {
            builder.withSampler(sampler);
        }
        for (RenderPipeline.UniformDescription uniform : base.getUniforms()) {
            if (uniform.textureFormat() != null) {
                builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
            } else {
                builder.withUniform(uniform.name(), uniform.type());
            }
        }
        return builder.build();
    }
}
