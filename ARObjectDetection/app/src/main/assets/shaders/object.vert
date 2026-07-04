#version 300 es

layout(location = 0) in vec4 a_Position;
layout(location = 1) in vec2 a_TexCoord;

uniform mat4 u_ModelViewProjection;

out vec2 v_TexCoord;

void main() {
    v_TexCoord = a_TexCoord;
    gl_Position = u_ModelViewProjection * a_Position;
}
