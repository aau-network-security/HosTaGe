precision mediump float;

uniform vec4 color;
uniform sampler2D texture;

varying vec3 vertexNormal;
varying vec2 vertexTexCoord;

void main() {
	vec3 normal = normalize(vertexNormal);
	float lambert = max(0.0, -normal.z);
	float rim = 1.0 - lambert;
	rim *= rim * rim;
	vec4 texColor = texture2D(texture, vertexTexCoord);
	gl_FragColor = rim + (0.4 + 0.4 * lambert) * texColor * color;
}
