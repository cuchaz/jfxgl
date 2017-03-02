#version 150 core

uniform sampler2D colorSampler;

in vec2 passTexCoord;

out vec4 outColor;

void main(void) {

	// not much to do here...
	
	vec4 color = texture(colorSampler, passTexCoord);
	
	outColor = color;
}
