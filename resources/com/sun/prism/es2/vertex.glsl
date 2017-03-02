#version 150 core

uniform vec2 viewSize;

in vec2 inPos;
in vec2 inTexCoord;

out vec2 passTexCoord;

void main(void) {

	// NOTE: opengl wants vertices in view space: [-1,1]x[-1,1]
	// but the fragment shaders work in screen space: [0,geometryWidth]x[0,geometryHeight]
	gl_Position = vec4(inPos*2/viewSize - 1, 0, 1);
	
	passTexCoord = inTexCoord;
}
