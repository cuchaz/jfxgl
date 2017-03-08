/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/
#version 150 core

uniform vec2 viewSize;
uniform vec2 viewPos;
uniform bool yflip;

in vec2 inPos;
in vec2 inTexCoord;

out vec2 passTexCoord;

void main(void) {

	vec2 pos = inPos + viewPos;
	
	if (yflip) {
		pos.y = viewSize.y - pos.y;
	}

	pos = pos*2/viewSize - 1;
	
	gl_Position = vec4(pos, 0, 1);
	
	passTexCoord = inTexCoord;
}
