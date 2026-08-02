#version 330

// Measures how much of the blur kernel each entity actually covers, which is a
// proxy for how large it is on screen.
//
// The halo radius is in screen pixels and fixed, so it does not shrink as a
// player walks away. A 26px halo around a player who is 200px tall is a glow;
// the same halo around a player 20px tall swallows them into a purple blob.
//
// A plain normalised blur of the silhouette gives ~0.5 at the edge of a large
// entity but only a small fraction for a distant one, because a tiny shape
// fills very little of the kernel. The combine pass uses that to fade the halo
// out with range while leaving the crisp outline untouched.
//
// Deliberately no amplification here, unlike outline_glow_blur: this pass is a
// measurement, and scaling it would destroy the very quantity being measured.

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

    float sum = 0.0;
    float weightSum = 0.0;

    for (float i = -radius; i <= radius; i += 1.0) {
        float weight = exp(-(i * i) / twoSigmaSq);
        sum += texture(InSampler, texCoord + sampleStep * i).a * weight;
        weightSum += weight;
    }

    fragColor = vec4(vec3(0.0), sum / weightSum);
}
