#version 330

// Lays the crisp core line over the soft rim halo.
//
// The colour is used exactly as configured — no renormalisation. An earlier
// version divided by the brightest channel to "recover" full strength, which
// meant every dark colour was forced back to a vivid one no matter what the
// config said.
//
// The halo is composited normally rather than screen-blended. Screen only
// brightens, so it renders a dark halo invisible; plain alpha lets any
// configured colour read as a real aura against bright terrain.

uniform sampler2D InSampler;   // crisp core line
uniform sampler2D GlowSampler; // soft rim halo
uniform sampler2D MaskSampler; // raw silhouette, to keep the halo outside it

in vec2 texCoord;

out vec4 fragColor;

// Halo opacity at its brightest, right against the line. Kept well under 1.0 on
// purpose: the halo's job is to make the silhouette pop out of the terrain, and
// the moment it approaches opaque it stops being a glow and starts being a
// sticker that hides the player inside it.
const float GLOW_STRENGTH = 1.00;
// Shapes how fast the halo fades with distance from the silhouette. Above 1.0
// gives a steady decay; below 1.0 flattens it toward a uniform band.
const float GLOW_GAMMA = 1.20;
// How solid the core line is. The line carries the shape information, so it is
// pushed all the way to opaque.
const float CORE_STRENGTH = 1.30;
// A touch of white in the core so the line reads as a lit edge rather than a
// flat stroke. Deliberately small: past ~0.3 the line stops carrying the
// configured hue at all and every colour setting looks the same.
const float CORE_WHITEN = 0.15;

void main() {
    vec4 core = texture(InSampler, texCoord);
    vec4 glow = texture(GlowSampler, texCoord);

    float coreAlpha = clamp(core.a * CORE_STRENGTH, 0.0, 1.0);
    float glowAlpha = clamp(pow(clamp(glow.a, 0.0, 1.0), GLOW_GAMMA) * GLOW_STRENGTH, 0.0, 1.0);

    // Confine the halo to the outside of the silhouette. A blur spreads inward
    // as well as outward, so on a distant player -- whose on-screen half-width
    // is smaller than the blur radius -- the two sides bleed past each other and
    // sum, filling the body into a solid lozenge with no readable shape. That is
    // what forced the radius down to the point where the glow vanished. Masking
    // by the silhouette makes the interior unfillable at any radius, so the halo
    // can be as wide as it needs to be for the near-range look.
    glowAlpha *= 1.0 - clamp(texture(MaskSampler, texCoord).a, 0.0, 1.0);

    vec3 coreColor = mix(core.rgb, vec3(1.0), CORE_WHITEN);
    vec3 glowColor = glow.rgb;

    // Proper "over": core on top of halo. Mixing the colours by coreAlpha while
    // taking max() of the alphas is not the same operator — wherever the halo is
    // stronger than the line it produces the halo's alpha carrying a colour
    // dragged most of the way to the whitened core, which washes the whole
    // silhouette out to a pale blob.
    float outAlpha = coreAlpha + glowAlpha * (1.0 - coreAlpha);
    vec3 outColor = outAlpha > 0.0001
        ? (coreColor * coreAlpha + glowColor * glowAlpha * (1.0 - coreAlpha)) / outAlpha
        : vec3(0.0);

    fragColor = vec4(outColor, outAlpha);
}
