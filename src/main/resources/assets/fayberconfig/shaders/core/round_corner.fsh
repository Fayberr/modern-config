#version 330

// Anti-aliased rounded-corner quadrant for Fayber Config.
//
// The GUI never gives a fragment shader the rectangle's size or radius, so the geometry is
// encoded in the UVs instead: a corner quad is blitted with texCoord0 covering one quadrant of
// the unit square, i.e. p = texCoord0 * 2 - 1 is the position relative to the corner's circle
// centre, in units of the radius. Coverage is then 1 inside the unit circle, 0 outside, with the
// transition exactly one screen pixel wide thanks to fwidth(). That makes the corner smooth at
// any GUI scale, any radius and any resolution, with no textures and no per-draw uniforms.

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 p = texCoord0 * 2.0 - 1.0;
    float d = length(p);

    // One-pixel-wide edge in screen space, whatever the quad's on-screen size is.
    float aa = fwidth(d);
    float coverage = 1.0 - smoothstep(1.0 - aa, 1.0 + aa, d);
    if (coverage <= 0.0) {
        discard;
    }

    // Sampler0 is bound to a plain white texture: keeping it in the shader means the sampler the
    // pipeline declares is actually used, so the uniform never gets optimised away.
    vec4 color = texture(Sampler0, vec2(0.5)) * vertexColor * ColorModulator;
    fragColor = vec4(color.rgb, color.a * coverage);
}
