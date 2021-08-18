package dk.aau.netsec.hostage.ui.fragment.opengl;

/**
 * some basic quaternion class because android doesn't provide any
 */
public class Quaternion {
	public final float w;
    public final float x;
    public final float y;
    public final float z;
	
	Quaternion() { // identity
		w = 1.0f; x = y = z = 0.0f;
	}
	
	Quaternion(float w, float x, float y, float z) {
		this.w = w;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Quaternion clone() {
		return new Quaternion(w, x, y, z);
	}
	
	public Quaternion multiply(final Quaternion p) {
		Quaternion q = this;
		return new Quaternion(
				q.w * p.w - q.x * p.x - q.y * p.y - q.z * p.z,
				q.w * p.x + q.x * p.w + q.y * p.z - q.z * p.y,
				q.w * p.y + q.y * p.w + q.z * p.x - q.x * p.z,
				q.w * p.z + q.z * p.w + q.x * p.y - q.y * p.x);
	}
	
	public static float[] cross(final float[] v1, final float[] v2) {
		float[] v3 = new float[3];
		v3[0] = v1[1] * v2[2] - v1[2] * v2[1];
		v3[1] = v1[2] * v2[0] - v1[0] * v2[2];
		v3[2] = v1[0] * v2[1] - v1[1] * v2[0];	
		return v3;
	}
	
	public static float[] multiply(final float s, final float[] v) {
		float[] result = new float[3];
		result[0] = s * v[0];
		result[1] = s * v[1];
		result[2] = s * v[2];
		return result;
	}
	
	public static float[] add(final float[] v1, final float[] v2) {
		float[] v3 = new float[3];
		v3[0] = v1[0] + v2[0];
		v3[1] = v1[1] + v2[1];
		v3[2] = v1[2] + v2[2];
		return v3;
	}
	
	public float[] multiply(final float[] v) { // rotate a point
		Quaternion q = this;
		float[] axis = new float[3];
		axis[0] = q.x; axis[1] = q.y; axis[2] = q.z;
		
		float[] uv = cross(axis, v);
		float[] uuv = cross(axis, uv);
		
		uv = multiply(2.0f * q.w, uv);
		uuv = multiply(2.0f, uuv);

		return add(v, add(uv, uuv));
	}
}
