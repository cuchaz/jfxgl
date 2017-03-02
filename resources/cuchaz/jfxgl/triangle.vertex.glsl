#version 150 core

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

in vec3 inPos;
in vec3 inColor;

out vec3 passColor;

void main(void) {

    gl_Position = projection*view*model*vec4(inPos, 1.0);
	
	passColor = inColor;
}
