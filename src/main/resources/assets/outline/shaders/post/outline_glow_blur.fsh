#version 330

// Wide separable Gaussian blur that turns the outline edge into a soft rim of
// light around the silhouette. Vanilla's entity_outline_box_blur declares a
// Radius uniform but then shadows it with a hardcoded `float radius = 2.0`,
// which is why the vanilla glow can never be wider than two pixels. This one
// actually honours the uniform.
//
// Only the halo uses this shader; the crisp line is built by outline_core_blur,
// which needs the opposite gain. See the comment there.

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

// The halo is light, not paint: it has to stay translucent even at its
// brightest so terrain still reads through it. sqrt(radius) per pass undoes the
// kernel's spreading of a one-pixel edge; GAIN then deliberately undershoots so
// the profile stays a gradient instead of clamping into an opaque plateau with
// a hard outer edge.
const float GAIN = 0.5;

void main() {
    vec2 sampleStep = (1.0 / InSize) * BlurDir;

    float radius = max(Radius, 1.0);

    // radius / 3.0, not radius * 0.5. The loop truncates the kernel at
    // +-radius, so sigma decides how much weight is thrown away at the cut: at
    // 2 sigma that is exp(-2) = 13.5% of the peak, which ends the halo in a
    // visible hard ring at exactly `radius` pixels. At 3 sigma it is
    // exp(-4.5) = 1.1%, low enough that the halo decays into nothing.
    float sigma = max(radius / 3.0, 0.5);
    float twoSigmaSq = 2.0 * sigma * sigma;

    vec3 colorSum = vec3(0.0);
    float alphaSum = 0.0;
    float weightSum = 0.0;

    for (float i = -radius; i <= radius; i += 1.0) {
        float weight = exp(-(i * i) / twoSigmaSq);
        vec4 texel = texture(InSampler, texCoord + sampleStep * i);

        // Weight colour by alpha so fully transparent texels cannot bleed
        // black into the halo.
        colorSum += texel.rgb * texel.a * weight;
        alphaSum += texel.a * weight;
        weightSum += weight;
    }

    vec3 color = alphaSum > 0.0001 ? colorSum / alphaSum : vec3(0.0);
    float alpha = clamp((alphaSum / weightSum) * sqrt(radius) * GAIN, 0.0, 1.0);

    fragColor = vec4(color, alpha);
}
