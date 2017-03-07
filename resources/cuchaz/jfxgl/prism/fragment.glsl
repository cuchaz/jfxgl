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

uniform sampler2D colorSampler;

in vec2 passTexCoord;

out vec4 outColor;

void main(void) {

	// not much to do here...
	
	vec4 color = texture(colorSampler, passTexCoord);
	
	outColor = color;
}
