#version 150 core

in vec3 passColor;

out vec4 outColor;

void main(void) {

	// not much to do here...
	vec4 color = vec4(passColor, 1);
	
	outColor = color;
}
