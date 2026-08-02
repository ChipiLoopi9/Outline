#version 330

// Lays the crisp core line over the wide halo and composites the halo as
// LIGHT rather than as paint.
//
// Vanilla merges the entity_outline target with straight alpha blending:
//
//     final = C * A + S * (1 - A)
//
// That can only replace scene colour. Against a dark scene, replacing a near
// black pixel with violet reads as a glow; against bright terrain the same
// alpha replaces a sunlit pixel with violet, which is a flat purple band and
// actually darkens it. So instead of picking C as "the glow colour", solve for
// the C that makes vanilla's own blend produce a screen composite:
//
//     desired = screen(S, glow) = 1 - (1 - S) * (1 - glow)
//     C       = (desired - S * (1 - A)) / A
//
// screen() only ever brightens, so the halo glows on dark scenes and quietly
// brightens bright ones instead of tinting them. The division is safe: glow
// energy is itself proportional to A, so C stays bounded as A approaches 0.

uniform sampler2D InSampler;   // crisp 2px core line
uniform sampler2D GlowSampler; // wide soft halo
uniform sampler2D MainSampler; // the scene as drawn so far

in vec2 texCoord;

out vec4 fragColor;

// Halo intensity. Higher is safe here in a way it was not under plain alpha
// blending, because screen() cannot push past white.
const float GLOW_STRENGTH = 1.15;
// Shapes the falloff. Above 1.0 keeps the halo tight to the line.
const float GLOW_GAMMA = 1.15;
// How much white is mixed into the core line. The reference line is nearly
// white while its halo stays violet, so this runs high on purpose.
const float CORE_WHITEN = 0.88;

// Renormalise to full brightness; the sobel pass dims the entity colour and we
// want the configured colour at full strength regardless.
vec3 fullBright(vec3 color) {
    float peak = max(max(color.r, color.g), color.b);
    return peak > 0.004 ? color / peak : vec3(0.0);
}

void main() {
    vec4 core = texture(InSampler, texCoord);
    vec4 glow = texture(GlowSampler, texCoord);
    vec3 scene = texture(MainSampler, texCoord).rgb;

    float glowAlpha = clamp(pow(clamp(glow.a, 0.0, 1.0), GLOW_GAMMA) * GLOW_STRENGTH, 0.0, 1.0);
    float coreAlpha = clamp(core.a, 0.0, 1.0);

    // Halo keeps the configured colour; the core line runs nearly white.
    vec3 haloColor = fullBright(glow.rgb);
    vec3 coreColor = mix(fullBright(core.rgb), vec3(1.0), CORE_WHITEN);

    vec3 glowEnergy = haloColor * glowAlpha;
    vec3 screened = 1.0 - (1.0 - scene) * (1.0 - glowEnergy);

    vec3 desired = mix(screened, coreColor, coreAlpha);

    float outAlpha = max(coreAlpha, glowAlpha);
    vec3 outColor = outAlpha > 0.001
        ? clamp((desired - scene * (1.0 - outAlpha)) / outAlpha, 0.0, 1.0)
        : scene;

    fragColor = vec4(outColor, outAlpha);
}
