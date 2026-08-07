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

uniform sampler2D InSampler;       // crisp core line
uniform sampler2D GlowSampler;     // soft rim halo, wide
uniform sampler2D GlowNearSampler; // same halo at a small radius, for distant targets
uniform sampler2D MaskSampler;     // raw silhouette, to keep the halo outside it

in vec2 texCoord;

out vec4 fragColor;

// Halo opacity at its brightest, right against the line. Kept well under 1.0 on
// purpose: the halo's job is to make the silhouette pop out of the terrain, and
// the moment it approaches opaque it stops being a glow and starts being a
// sticker that hides the player inside it.
const float GLOW_STRENGTH = 1.30;
// Shapes how fast the halo fades with distance from the silhouette. Above 1.0
// gives a steady decay; below 1.0 carries more of the halo out to its full
// width. Safe to sit under 1.0 here only because the halo comes from a blurred
// solid, whose profile outside the body is already a clean monotonic falloff --
// lifting it stretches the gradient rather than flattening it into a band.
const float GLOW_GAMMA = 0.80;
// How far the halo's colour is pushed toward the pure form of its own hue.
//
// A glow is light, not paint. The fill colour is deliberately dark so the
// occluded body reads as a solid shape, but spreading that same dark violet
// over sunlit dirt just darkens it -- the halo lands as a smudge or a shadow
// instead of something glowing. Normalising toward the brightest form of the
// configured hue keeps the colour the user asked for and lets it read as light
// against bright terrain. Only the halo is lifted; the core line stays exactly
// as configured, so the colour setting still governs what the outline looks
// like.
const float GLOW_LIFT = 0.55;
// How solid the core line is. The line carries the shape information, so it is
// pushed all the way to opaque.
const float CORE_STRENGTH = 1.0;
// A touch of white in the core so the line reads as a lit edge rather than a
// flat stroke. Deliberately small: past ~0.3 the line stops carrying the
// configured hue at all and every colour setting looks the same.
const float CORE_WHITEN = 0.3;

void main() {
    vec4 core = texture(InSampler, texCoord);

    // Two halo widths, and whichever is stronger here wins.
    //
    // A blurred solid only reaches ~0.5 alpha on its boundary while the shape is
    // wide compared to the kernel. Once it is not -- a player far enough away to
    // be a few pixels across -- the blur spreads what little alpha there is over
    // the whole radius and the glow thins out to nothing. That is why the glow
    // faded with distance while the outline stayed put.
    //
    // The narrow blur is still saturated at those sizes, so it carries the far
    // field. On a nearby player it is fully contained inside the wide halo and
    // max() discards it, which is what keeps the close-up look untouched.
    vec4 glowWide = texture(GlowSampler, texCoord);
    vec4 glowNear = texture(GlowNearSampler, texCoord);
    // Pick, don't average: both carry the same colour where they carry anything,
    // but the weaker one trends to rgb 0 as its alpha vanishes, so mixing would
    // drag the halo toward black exactly where it is already faintest.
    vec4 glow = glowNear.a > glowWide.a ? glowNear : glowWide;

    float coreAlpha = clamp(core.a * CORE_STRENGTH, 0.0, 1.0);
    // Gamma before strength, not after. Shaping the raw falloff and then
    // scaling it keeps STRENGTH a plain brightness knob; doing it the other way
    // round makes the two constants fight, because gamma pulls anything under
    // 1.0 back down by an amount that depends on how far strength pushed it up.
    float glowAlpha = clamp(pow(clamp(glow.a, 0.0, 1.0), GLOW_GAMMA) * GLOW_STRENGTH, 0.0, 1.0);

    // Confine the halo to the outside of the silhouette. The halo is a blur of
    // the filled body, so inside the body it sits near fully opaque -- without
    // this the effect would paint over the player rather than surround them.
    // It also keeps the width honest at range: a blur spreads inward as well as
    // outward, so on a distant player, whose on-screen half-width is smaller
    // than the radius, the two sides would otherwise bleed past each other and
    // sum into a solid lozenge with no readable shape.
    glowAlpha *= 1.0 - clamp(texture(MaskSampler, texCoord).a, 0.0, 1.0);

    vec3 coreColor = mix(core.rgb, vec3(1.0), CORE_WHITEN);

    // Brightest form of the halo's own hue: scale the channels up until the
    // largest hits 1.0. This is a value change only, so a violet stays violet
    // and a red stays red -- unlike the renormalisation an earlier version
    // applied to the whole output, which forced every configured colour to full
    // intensity and made the setting meaningless.
    float peak = max(glow.r, max(glow.g, glow.b));
    vec3 glowColor = peak > 0.0001 ? mix(glow.rgb, glow.rgb / peak, GLOW_LIFT) : glow.rgb;

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
