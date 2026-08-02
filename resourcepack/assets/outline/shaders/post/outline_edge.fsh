#version 330

// Edge detect that preserves the configured colour exactly.
//
// Vanilla's entity_sobel emits `outColor * 0.2`, an unnormalised sum of five
// taps, so the colour it produces depends on how many neighbours happen to sit
// inside the silhouette. Dividing by the coverage instead returns the entity's
// actual colour, which is what lets a dark colour stay dark instead of drifting
// toward whatever the tap count implies.

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 oneTexel = 1.0 / InSize;

    vec4 c = texture(InSampler, texCoord);
    vec4 l = texture(InSampler, texCoord - vec2(oneTexel.x, 0.0));
    vec4 r = texture(InSampler, texCoord + vec2(oneTexel.x, 0.0));
    vec4 u = texture(InSampler, texCoord - vec2(0.0, oneTexel.y));
    vec4 d = texture(InSampler, texCoord + vec2(0.0, oneTexel.y));

    float edge = clamp(abs(c.a - l.a) + abs(c.a - r.a)
                     + abs(c.a - u.a) + abs(c.a - d.a), 0.0, 1.0);

    float coverage = c.a + l.a + r.a + u.a + d.a;
    vec3 color = coverage > 0.001
        ? (c.rgb * c.a + l.rgb * l.a + r.rgb * r.a + u.rgb * u.a + d.rgb * d.a) / coverage
        : vec3(0.0);

    fragColor = vec4(color, edge);
}
