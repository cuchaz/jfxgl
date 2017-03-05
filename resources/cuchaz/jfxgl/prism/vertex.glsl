#version 150 core

uniform vec2 viewSize;
uniform vec2 viewPos;
uniform bool yflip;

in vec2 inPos;
in vec2 inTexCoord;

out vec2 passTexCoord;

void main(void) {

	vec2 pos = inPos;
	
	if (yflip) {
		pos.y = viewSize.y - pos.y;
	}

	pos = pos*2/viewSize - 1;
	
	gl_Position = vec4(pos, 0, 1);
	
	passTexCoord = inTexCoord;
}
