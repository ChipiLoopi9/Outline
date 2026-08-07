#version 330

// Verbatim copy of the raw silhouette into a target of its own.
//
// Both the halo blur and the final composite need the unmodified silhouette:
// the blur uses it as the source of the glow, and the composite uses it to keep
// that glow outside the body. The composite also writes back to
// minecraft:entity_outline, so sampling that same target inside it would mean
// reading and writing one attachment in a single pass. Every vanilla chain
// ping-pongs between two targets rather than doing that, so there is no
// guarantee the framegraph keeps the read and the write apart. One extra fetch
// per pixel buys a chain that cannot depend on the answer.

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    fragColor = texture(InSampler, texCoord);
}
