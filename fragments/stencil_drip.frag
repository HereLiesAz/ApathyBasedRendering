#version 300 es
precision highp float;

in vec2 vUV;
uniform sampler2D uBaseTexture;
uniform sampler2D uNoiseTexture;
uniform sampler2D uStencilMask;
uniform float uTime;

out vec4 FragColor;

void main() {
    // Noise-driven gravity for the drip. 
    // High frequency UVs pull from the noise map to create unpredictable, bleeding vectors.
    vec2 noiseUV = vUV * 5.0;
    float noiseVal = texture(uNoiseTexture, noiseUV).r;
    
    // Y-axis decay dragging the paint down. Time makes it bleed.
    float dripFactor = noiseVal * (sin(uTime * 0.2) * 0.5 + 0.5) * 0.15;
    vec2 distortedUV = vUV + vec2(0.0, -dripFactor);
    
    // The cut. A binary worldview. It is either paint or it is void.
    float stencilVal = texture(uStencilMask, vUV).r;
    float binaryCut = step(0.5, stencilVal); 
    
    // Sample the distorted reality.
    vec4 rawColor = texture(uBaseTexture, distortedUV);
    
    // Strip the hope. Monochromatic conversion.
    float luminance = dot(rawColor.rgb, vec3(0.299, 0.587, 0.114));
    vec3 apathyColor = vec3(luminance);
    
    // Multiply by the binary cut to slice out the stencil.
    FragColor = vec4(apathyColor * binaryCut, rawColor.a * binaryCut);
}
