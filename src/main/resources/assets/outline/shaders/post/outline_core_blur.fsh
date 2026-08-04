#version 330

// Tight blur that turns the raw edge mask into the crisp core line.
//
// This is deliberately a separate shader from outline_glow_blur even though the
// kernel is the same shape. The two passes want opposite things from their
// gain: the core has to come out of the blur essentially opaque so it reads as
// a drawn line, while the halo has to stay translucent so it reads as light.
// A single shared gain cannot do both, and when the halo's gain was applied to
// the core the line only reached ~0.43 alpha and the whole effect turned to
// mush.
//
// It is also not vanilla's entity_outline_box_blur: that one averages RGB
// without weighting by alpha, so the transparent (RGB = 0) texels around the
// edge bleed black into the line and darken the configured colour.

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurConfig {
    vec2 BlurDir;
    float Radius;
};

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

// Restores the line to full opacity after two passes. The normalised kernel
// spreads a one-pixel edge out to roughly 1/radius, so sqrt(radius) per pass
// undoes the spreading; GAIN then pushes the result to the clamp so the line
// is solid rather than a 40%-alpha smear.
const float GAIN = 1.1;

void main() {
    vec2 sampleStep = (1.0 / InSize) * BlurDir;

    float radius = max(Radius, 1.0);
    float sigma = max(radius / 3.0, 0.5);
    float twoSigmaSq = 2.0 * sigma * sigma;

    vec3 colorSum = vec3(0.0);
    float alphaSum = 0.0;
    float weightSum = 0.0;

    for (float i = -radius; i <= radius; i += 1.0) {
        float weight = exp(-(i * i) / twoSigmaSq);
        vec4 texel = texture(InSampler, texCoord + sampleStep * i);

        colorSum += texel.rgb * texel.a * weight;
        alphaSum += texel.a * weight;
        weightSum += weight;
    }

    vec3 color = alphaSum > 0.0001 ? colorSum / alphaSum : vec3(0.0);
    float alpha = clamp((alphaSum / weightSum) * sqrt(radius) * GAIN, 0.0, 1.0);

    fragColor = vec4(color, alpha);
}
