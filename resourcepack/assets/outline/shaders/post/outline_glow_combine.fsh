#version 330

// Lays the crisp core line over the wide halo.
//
// The colour is used exactly as configured — no renormalisation. An earlier
// version divided by the brightest channel to "recover" full strength, which
// meant every dark purple was forced back to vivid magenta no matter what the
// config said. A dark colour has to be allowed to stay dark.
//
// The halo is composited normally rather than screen-blended. Screen only
// brightens, so it renders a dark halo invisible; plain alpha lets a deep
// purple read as a real aura against bright terrain.

uniform sampler2D InSampler;   // crisp core line
uniform sampler2D GlowSampler; // wide soft halo
uniform sampler2D SizeSampler; // how much of the kernel the entity covers

in vec2 texCoord;

out vec4 fragColor;

// How much kernel coverage is needed before the halo is drawn at all, and where
// it reaches full strength. The halo radius is fixed in screen pixels, so a
// distant player would otherwise get the same wide halo as a near one and turn
// into a featureless blob. Fading it out below these thresholds keeps the crisp
// outline at range while the glow stays a near-range effect.
const float SIZE_FADE_LO = 0.10;
const float SIZE_FADE_HI = 0.34;

// Halo opacity at its brightest, right against the line.
const float GLOW_STRENGTH = 1.15;
// Shapes how fast the halo fades with distance from the silhouette. Above 1.0
// gives a steady decay; below 1.0 flattens it toward a uniform band.
const float GLOW_GAMMA = 1.90;
// How solid the core line is.
const float CORE_STRENGTH = 1.10;
// How much white is mixed into the core line so it reads as a defined edge.
// Kept low so the line stays purple rather than washing out.
const float CORE_WHITEN = 0.30;

void main() {
    vec4 core = texture(InSampler, texCoord);
    vec4 glow = texture(GlowSampler, texCoord);

    float coverage = texture(SizeSampler, texCoord).a;
    float sizeFade = smoothstep(SIZE_FADE_LO, SIZE_FADE_HI, coverage);

    float coreAlpha = clamp(core.a * CORE_STRENGTH, 0.0, 1.0);
    float glowAlpha = clamp(pow(clamp(glow.a, 0.0, 1.0), GLOW_GAMMA) * GLOW_STRENGTH, 0.0, 1.0) * sizeFade;

    vec3 coreColor = mix(core.rgb, vec3(1.0), CORE_WHITEN);
    vec3 glowColor = glow.rgb;

    fragColor = vec4(mix(glowColor, coreColor, coreAlpha), max(coreAlpha, glowAlpha));
}
