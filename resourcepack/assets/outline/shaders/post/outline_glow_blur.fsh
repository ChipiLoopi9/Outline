#version 330

// Wide separable Gaussian blur used to turn the one-pixel outline edge into a
// soft halo. Vanilla's entity_outline_box_blur declares a Radius uniform but
// then shadows it with a hardcoded `float radius = 2.0`, which is why the
// vanilla glow can never be wider than two pixels. This one actually honours
// the uniform.

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

void main() {
    vec2 oneTexel = 1.0 / InSize;
    vec2 sampleStep = oneTexel * BlurDir;

    float radius = max(Radius, 1.0);
    float sigma = max(radius * 0.5, 0.5);
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

    float alpha = alphaSum / weightSum;
    vec3 color = alphaSum > 0.0001 ? colorSum / alphaSum : vec3(0.0);

    // A one-pixel edge spread over this kernel peaks at roughly 0.8/radius, so
    // sqrt(radius) per pass brings the two-pass peak back to ~0.8 whatever the
    // radius. Amplifying here rather than in the combine also keeps the halo
    // out of the low end of the 8-bit target, where it would band badly.
    alpha = clamp(alpha * sqrt(radius), 0.0, 1.0);

    fragColor = vec4(color, alpha);
}
