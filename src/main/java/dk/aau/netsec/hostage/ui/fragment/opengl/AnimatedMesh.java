package dk.aau.netsec.hostage.ui.fragment.opengl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

/**
 * @author Fabio Arnold
 * <p>
 * Animated Mesh
 * This class reads a mesh in the AMSH binary format and creates the necessary OpenGL objects for drawing
 */

public class AnimatedMesh {

    private int vertexSize;
    private int triangleCount;

    private int vertexBuffer; // vbo
    private int indexBuffer;

    private ArrayList<Bone> bones;
    private float[] matrices; // matrix palette for skinning

    private ArrayList<Action> actions;
    private Action currentAction;
    private int currentFrame;
    private boolean loopAction = false;
    private boolean reverseAction = false;

    /**
     * start play an animation (action)
     *
     * @param actionName name in the AMSH file
     * @param loop       loop the animation
     * @param reverse    play the animation backwards
     */
    public void startAction(String actionName, boolean loop, boolean reverse) {
        if (!(currentAction != null && currentAction.name.equals(actionName) && reverse)) // keep the current frame
            currentFrame = 0;
        loopAction = loop;
        reverseAction = reverse;
        currentAction = null;
        // find the action
        for (Action action : actions) {
            if (action.name.equals(actionName)) {
                currentAction = action;
                break;
            }
        }
        if (currentAction != null && reverseAction)
            if (!(currentAction.name.equals(actionName))) // keep the current frame
                currentFrame = currentAction.numFrames - 1;
    }

    /**
     * @return true if completed OR hasn't started yet.
     */
    public boolean isActionDone() {
        if (currentAction == null)
            return true;
        return currentFrame <= 1 || currentFrame >= currentAction.numFrames - 1;
		/*
		if (reverseAction)
			return currentFrame <= 0;
		else
			return currentFrame >= currentAction.numFrames - 1;
			*/
    }

    // private classes. don't use these.

    private static class Bone {
        @SuppressWarnings("unused")
        public String name; // 15 bytes
        public final int parentIndex; // 1 byte
        public final float[] invBindPose; // 64 bytes

        Bone(ByteBuffer data) {
            name = "";
            boolean stop = false;
            for (int i = 0; i < 15; i++) {
                char c;
                if ((c = (char) data.get()) == '\0') stop = true;
                if (!stop) name += c;
            }
            // Log.i("bone", name);
            parentIndex = data.get();
            invBindPose = new float[16];
            data.asFloatBuffer().get(invBindPose);
            data.position(data.position() + 64);
        }
    }

    private class Action {
        public String name; // 16 bytes
        public final int numFrames;
        public final ArrayList<Track> tracks;

        public static final int kHeaderSize = 28;

        Action(ByteBuffer data) {
            name = "";
            boolean stop = false;
            for (int i = 0; i < 16; i++) {
                char c;
                if ((c = (char) data.get()) == '\0') stop = true;
                if (!stop) name += c;
            }
            Log.i("action", name);
            numFrames = data.getInt();
            int trackOffset = data.getInt();
            int trackCount = data.getInt();
            tracks = new ArrayList<>();
            for (int i = 0; i < trackCount; i++) {
                data.position(trackOffset + i * Track.kHeaderSize);
                tracks.add(new Track(data));
            }
        }
    }

    private static class Track {
        @SuppressWarnings("unused")
        public int boneIndex;
        public final ArrayList<JointPose> poses;

        public static final int kHeaderSize = 12;

        Track(ByteBuffer data) {
            boneIndex = data.getInt();
            int jointPoseOffset = data.getInt();
            int jointPoseCount = data.getInt();
            poses = new ArrayList<>();
            data.position(jointPoseOffset);
            for (int i = 0; i < jointPoseCount; i++) {
                //data.position(jointPoseOffset + i * JointPose::kSize); // joint pose size == 32
                poses.add(new JointPose(data));
            }
        }
    }

    private static class JointPose {
        public Quaternion rotation;
        public float[] translation;
        @SuppressWarnings("unused")
        float scale;

        public static final int kSize = 32;

        JointPose() { // empty pose == identity
            rotation = new Quaternion();
            translation = new float[3];
            translation[0] = translation[1] = translation[2] = 0.0f;
            scale = 1.0f;
        }

        JointPose(ByteBuffer data) {
            FloatBuffer floatData = data.asFloatBuffer();
            data.position(data.position() + kSize);

            // quat data is x y z w, because of glm
            float x = floatData.get();
            float y = floatData.get();
            float z = floatData.get();
            float w = floatData.get();
            rotation = new Quaternion(w, x, y, z);
            translation = new float[3];
            floatData.get(translation);
            scale = floatData.get();
        }

        float[] toMatrix() { // TODO: scale
            float[] matrix = new float[16];
            Matrix.setIdentityM(matrix, 0);

            Quaternion q = rotation;
            matrix[0 * 4 + 0] = 1.0f - 2.0f * q.y * q.y - 2.0f * q.z * q.z;
            matrix[0 * 4 + 1] = 2.0f * q.x * q.y + 2.0f * q.w * q.z;
            matrix[0 * 4 + 2] = 2.0f * q.x * q.z - 2.0f * q.w * q.y;

            matrix[1 * 4 + 0] = 2.0f * q.x * q.y - 2.0f * q.w * q.z;
            matrix[1 * 4 + 1] = 1.0f - 2.0f * q.x * q.x - 2.0f * q.z * q.z;
            matrix[1 * 4 + 2] = 2.0f * q.y * q.z + 2.0f * q.w * q.x;

            matrix[2 * 4 + 0] = 2.0f * q.x * q.z + 2 * q.w * q.y;
            matrix[2 * 4 + 1] = 2.0f * q.y * q.z - 2 * q.w * q.x;
            matrix[2 * 4 + 2] = 1.0f - 2.0f * q.x * q.x - 2.0f * q.y * q.y;

            matrix[3 * 4 + 0] = translation[0];
            matrix[3 * 4 + 1] = translation[1];
            matrix[3 * 4 + 2] = translation[2];

            return matrix;
        }
    }

    /**
     * create an animmesh from an inputstream
     *
     * @param is the inputstream
     */
    public AnimatedMesh(InputStream is) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            final int EOF = -1;
            int len;
            byte[] buffer = new byte[1 << 12];
            while (EOF != (len = is.read(buffer)))
                out.write(buffer, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        //data = ByteBuffer.wrap(out.toByteArray()); // doesn't work data needs to be direct
        ByteBuffer data = ByteBuffer.allocateDirect(out.size());
        data.order(ByteOrder.nativeOrder());
        data.put(out.toByteArray());
        data.position(0);

        // header
        int magicNum = data.getInt();
        int version = data.getInt();
        //assert(magicNum == ('A' << 24 | 'M' << 16 | 'S' << 8 | 'H') && version == 1);

        vertexSize = 48;
        int vertexOffset = data.getInt();
        int vertexCount = data.getInt();
        int triangleOffset = data.getInt();
        triangleCount = data.getInt();
        int boneOffset = data.getInt();
        int boneCount = data.getInt();
        int actionOffset = data.getInt();
        int actionCount = data.getInt();

        // vertices and indices data
        IntBuffer buffers = IntBuffer.allocate(2);
        GLES20.glGenBuffers(2, buffers); // buffer names
        vertexBuffer = buffers.get();
        indexBuffer = buffers.get();

        data.position(vertexOffset);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexSize * vertexCount, data.asFloatBuffer(), GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // convert int indices to short
        // TODO: more efficient way?
        data.position(triangleOffset);
        ShortBuffer indexBufferData = ShortBuffer.allocate(3 * triangleCount);
        for (int i = 0; i < 3 * triangleCount; i++)
            indexBufferData.put((short) data.getInt());
        indexBufferData.position(0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 6 * triangleCount, indexBufferData, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        // bones
        bones = new ArrayList<>();
        data.position(boneOffset);
        for (int i = 0; i < boneCount; i++)
            bones.add(new Bone(data));

        matrices = new float[16 * boneCount];

        // actions
        actions = new ArrayList<>();
        for (int i = 0; i < actionCount; i++) {
            data.position(actionOffset + i * Action.kHeaderSize); // action header size == 28
            actions.add(new Action(data));
        }

        currentAction = null;
        currentFrame = 0;
        loopAction = false;
    }

    public static float[] addVec3(final float[] v1, final float[] v2) {
        float[] v3 = new float[3];
        v3[0] = v1[0] + v2[0];
        v3[1] = v1[1] + v2[1];
        v3[2] = v1[2] + v2[2];
        return v3;
    }

    /**
     * update the bone transforms of the mesh and advance one frame
     */
    public void tick() {
        // empty pose
        ArrayList<JointPose> pose = new ArrayList<>();
        for (int i = 0; i < bones.size(); i++)
            pose.add(new JointPose());

        if (currentAction != null) {
            // fill pose with action
            for (int i = 0; i < currentAction.tracks.size(); i++) {
                // TODO: do lerp or something nice
                pose.get(i).rotation = currentAction.tracks.get(i).poses.get(currentFrame).rotation;
                pose.get(i).translation = currentAction.tracks.get(i).poses.get(currentFrame).translation;
            }

            // advance one frame
            if (reverseAction) {
                if (currentFrame > 0) {
                    currentFrame--;
                } else if (loopAction) {
                    currentFrame = currentAction.numFrames - 1;
                }
            } else {
                if (currentFrame < currentAction.numFrames - 1) {
                    currentFrame++;
                } else if (loopAction) {
                    currentFrame = 0;
                }
            }
        }

        // convert pose to skinning matrices
        for (int i = 0; i < bones.size(); i++) {
            int parentIndex = bones.get(i).parentIndex;
            if (parentIndex != -1) { // bone has parent
                JointPose parentPose = pose.get(parentIndex);
                pose.get(i).rotation = parentPose.rotation.multiply(pose.get(i).rotation);
                pose.get(i).translation = addVec3(parentPose.translation, parentPose.rotation.multiply(pose.get(i).translation));
            }
            Matrix.multiplyMM(matrices, i * 16, pose.get(i).toMatrix(), 0, bones.get(i).invBindPose, 0);
        }
    }

    /**
     * draws the mesh
     *
     * @param program the currently bound shader program
     */
    public void draw(int program) {
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "boneMatrices"), bones.size(), false, matrices, 0);

        // TODO: cache attrib locations
        int positionIndex = GLES20.glGetAttribLocation(program, "position");
        int normalIndex = GLES20.glGetAttribLocation(program, "normal");
        int texCoordIndex = GLES20.glGetAttribLocation(program, "texCoord");
        int boneIndicesIndex = GLES20.glGetAttribLocation(program, "boneIndices");
        int boneWeightsIndex = GLES20.glGetAttribLocation(program, "boneWeights");

        GLES20.glEnableVertexAttribArray(positionIndex);
        GLES20.glEnableVertexAttribArray(normalIndex);
        GLES20.glEnableVertexAttribArray(texCoordIndex);
        GLES20.glEnableVertexAttribArray(boneIndicesIndex);
        GLES20.glEnableVertexAttribArray(boneWeightsIndex);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer);
        GLES20.glVertexAttribPointer(positionIndex, 3, GLES20.GL_FLOAT, false, vertexSize, 0);
        GLES20.glVertexAttribPointer(normalIndex, 3, GLES20.GL_FLOAT, false, vertexSize, 12);
        GLES20.glVertexAttribPointer(texCoordIndex, 2, GLES20.GL_FLOAT, false, vertexSize, 24);
        GLES20.glVertexAttribPointer(boneIndicesIndex, 4, GLES20.GL_UNSIGNED_BYTE, false, vertexSize, 32);
        GLES20.glVertexAttribPointer(boneWeightsIndex, 3, GLES20.GL_FLOAT, false, vertexSize, 36);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3 * triangleCount, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glDisableVertexAttribArray(positionIndex);
        GLES20.glDisableVertexAttribArray(normalIndex);
        GLES20.glDisableVertexAttribArray(texCoordIndex);
        GLES20.glDisableVertexAttribArray(boneIndicesIndex);
        GLES20.glDisableVertexAttribArray(boneWeightsIndex);
    }
}
