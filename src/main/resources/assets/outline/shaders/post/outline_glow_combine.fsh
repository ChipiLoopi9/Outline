#version 330

// Lays the crisp outline over the wide halo: bright, slightly whitened core
// line on top, saturated purple bloom falling off around it.

uniform sampler2D InSampler;   // vanilla-style 2px outline
uniform sampler2D GlowSampler; // wide soft halo

in vec2 texCoord;

out vec4 fragColor;

// How far the halo is pushed before clipping. Raise for a heavier bloom.
const float GLOW_STRENGTH = 1.25;
// Below 1.0 this lifts the faint outer falloff without blowing out the middle.
const float GLOW_GAMMA = 0.70;
// How much white is mixed into the core line, 0.0 = pure outline colour.
const float CORE_WHITEN = 0.40;

// Renormalise to full brightness; the sobel pass dims the entity colour and we
// want the configured colour at full strength regardless.
vec3 fullBright(vec3 color) {
    float peak = max(max(color.r, color.g), color.b);
    return peak > 0.004 ? color / peak : vec3(0.0);
}

void main() {
    vec4 core = texture(InSampler, texCoord);
    vec4 glow = texture(GlowSampler, texCoord);

    vec3 coreColor = mix(fullBright(core.rgb), vec3(1.0), CORE_WHITEN);
    vec3 glowColor = fullBright(glow.rgb);

    float coreAlpha = clamp(core.a, 0.0, 1.0);
    float glowAlpha = clamp(pow(clamp(glow.a, 0.0, 1.0), GLOW_GAMMA) * GLOW_STRENGTH, 0.0, 1.0);

    float outAlpha = coreAlpha + glowAlpha * (1.0 - coreAlpha);
    vec3 outColor = outAlpha > 0.0001
        ? (coreColor * coreAlpha + glowColor * glowAlpha * (1.0 - coreAlpha)) / outAlpha
        : vec3(0.0);

    fragColor = vec4(outColor, outAlpha);
}
