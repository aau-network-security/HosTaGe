uniform mat4 mvp;
uniform mat4 boneMatrices[16];

attribute vec3 position;
attribute vec3 normal;
attribute vec2 texCoord;

attribute vec4 boneIndices; // should be ivec4
attribute vec3 boneWeights;

varying vec3 vertexNormal;
varying vec2 vertexTexCoord;

void main() {
	float fourthBoneWeight = 1.0 - boneWeights[0] - boneWeights[1] - boneWeights[2];

	mat4 boneMatrix = boneWeights[0] * boneMatrices[int(boneIndices.x)];
	boneMatrix += boneWeights[1] * boneMatrices[int(boneIndices.y)];
	boneMatrix += boneWeights[2] * boneMatrices[int(boneIndices.z)];
	boneMatrix += fourthBoneWeight * boneMatrices[int(boneIndices.w)];

	vertexNormal = mat3(mvp) * mat3(boneMatrix) * normal;
	vertexTexCoord = texCoord;
	gl_Position = mvp * boneMatrix * vec4(position, 1.0);
}
