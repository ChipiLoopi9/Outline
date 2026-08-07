#version 330

// Wide separable Gaussian that turns the silhouette into a soft rim of light.
//
// The input is the filled silhouette, not the one-pixel edge the crisp line is
// built from. Blurring a solid area puts alpha ~0.5 on its own boundary and
// decays smoothly outward over sigma, which is the halo shape wanted here and
// costs nothing to produce. Blurring a one-pixel line instead spreads a total
// alpha of 1 across the whole kernel, leaving a peak near 1/(sigma*sqrt(2pi))
// -- about 0.05 at these widths -- so it had to be multiplied back up, and the
// wider the halo the harder it was pushed. That is what kept the glow stuck at
// roughly ten pixels: past that, the gain needed to make it visible also
// clamped the near half to a flat opaque band.
//
// It also fixes the distance behaviour for free. A blurred solid scales with
// the shape, so a player twenty blocks away -- a few pixels tall -- spreads
// very little alpha and keeps a tight glow, instead of being swallowed by a
// halo sized in screen pixels.
//
// Vanilla's entity_outline_box_blur declares a Radius uniform and then shadows
// it with a hardcoded `float radius = 2.0`, which is why the vanilla glow can
// never be wider than two pixels. This one honours the uniform.

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

// Trims the halo's overall level. A blurred solid already carries its own
// strength, so unlike the core blur this needs no width-dependent correction --
// the value is a straight scale and 1.0 means "as the Gaussian left it".
const float GAIN = 1.0;

void main() {
    vec2 texelStep = (1.0 / InSize) * BlurDir;

    float radius = max(Radius, 1.0);

    // radius / 3.0, not radius * 0.5. The loop truncates the kernel at
    // +-radius, so sigma decides how much weight is thrown away at the cut: at
    // 2 sigma that is exp(-2) = 13.5% of the peak, which ends the halo in a
    // visible hard ring at exactly `radius` pixels. At 3 sigma it is
    // exp(-4.5) = 1.1%, low enough that the halo decays into nothing.
    float sigma = max(radius / 3.0, 0.5);
    float twoSigmaSq = 2.0 * sigma * sigma;

    // One tap per pixel across a halo this wide is a fullscreen cost paid for
    // detail the kernel cannot represent: at sigma 12 the profile barely moves
    // between neighbouring pixels. Striding keeps the tap count flat as the
    // radius grows, and the input is sampled bilinearly so each tap is an
    // average over the pixels it steps past rather than a point sample.
    float stride = max(floor(radius / 10.0), 1.0);

    vec3 colorSum = vec3(0.0);
    float alphaSum = 0.0;
    float weightSum = 0.0;

    for (float i = -radius; i <= radius; i += stride) {
        float weight = exp(-(i * i) / twoSigmaSq);
        vec4 texel = texture(InSampler, texCoord + texelStep * i);

        // Weight colour by alpha so fully transparent texels cannot bleed
        // black into the halo.
        colorSum += texel.rgb * texel.a * weight;
        alphaSum += texel.a * weight;
        weightSum += weight;
    }

    vec3 color = alphaSum > 0.0001 ? colorSum / alphaSum : vec3(0.0);
    float alpha = clamp((alphaSum / weightSum) * GAIN, 0.0, 1.0);

    fragColor = vec4(color, alpha);
}
