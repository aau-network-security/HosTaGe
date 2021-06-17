package dk.aau.netsec.hostage.ui.fragment.opengl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Scanner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import dk.aau.netsec.hostage.R;
import dk.aau.netsec.hostage.ui.activity.MainActivity;

/**
 * @author Fabio Arnold
 * <p>
 * ThreatIndicatorGLRenderer
 * This class is responsible for drawing an animation representing the current threat level.
 * Use the method setThreatLevel to set the state (0 to 3).
 */

public class ThreatIndicatorGLRenderer implements Renderer {
    public enum ThreatLevel {
        NOT_MONITORING,
        NO_THREAT,
        PAST_THREAT,
        LIVE_THREAT
    }

    /**
     * Set the threat level which should be indicated
     *
     * @param threatLevel
     */
    public static void setThreatLevel(ThreatLevel threatLevel) {
        mNextThreatLevel = threatLevel;
    }

    public static void showSpeechBubble() {
        bubbleWait = 3.0f; // 3 seconds;
    }

    /**
     * Match the background color of the view holding this renderer
     *
     * @param color 32 bit integer encoding the color
     */
    public static void setBackgroundColor(int color) {
        mBackgroundColor[0] = (float) Color.red(color) / 255.0f;
        mBackgroundColor[1] = (float) Color.green(color) / 255.0f;
        mBackgroundColor[2] = (float) Color.blue(color) / 255.0f;
    }

    private static float[] mBackgroundColor = new float[3];

    // OpenGL data
    private int mAnimatedProgram;
    private int mTexturedProgram;
    private float[] mModelview;
    private float[] mProjection;
    private float[] mMVP;

    private AnimatedMesh androidMesh = null;
    private AnimatedMesh beeMesh = null;
    private int androidTexture;
    private int beeTexture;
    private int speechBubbleTexture;
    private int mQuadVertexBuffer;

    private GLFont font = null;
    private static float bubbleAlpha = 0.0f;
    private static float bubbleWait = 0.0f;

    // threat state
    private static ThreatLevel mNextThreatLevel = ThreatLevel.NO_THREAT;

    private ThreatLevel mCurrentThreatLevel = ThreatLevel.NO_THREAT;
    private ThreatLevel mTargetThreatLevel = ThreatLevel.NO_THREAT;
    private float mThreatLevelTransition = 1.0f; // 1.0 means transition is complete

    private static boolean sPlayGreetingAnimation = true; // greet the first time

    private long mStartTimeMillis; // for animation

    public ThreatIndicatorGLRenderer() {
        mStartTimeMillis = System.currentTimeMillis();
    }

    /**
     * Initialization will be called after GL context is created and is current
     */
    public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
        GLES20.glClearColor(mBackgroundColor[0], mBackgroundColor[1], mBackgroundColor[2], 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        AssetManager assets = MainActivity.getInstance().getAssets();
        try {
            InputStream is = assets.open("meshes/android.amh");
            androidMesh = new AnimatedMesh(is);
			/* play the greeting animation the first time the gets opened
			   not each time the threatindicator gets created */
            if (sPlayGreetingAnimation) {
                androidMesh.startAction("greet", false, false);
                sPlayGreetingAnimation = false;
            } else if (mCurrentThreatLevel == ThreatLevel.NO_THREAT) { // default state
                androidMesh.startAction("happy", true, false); // play NO_THREAT animation
            }
        } catch (IOException e) {
            Log.e("gl", "Couldn't open android mesh");
        }
        androidTexture = loadTexture("textures/android-tex.png");
        try {
            InputStream is = assets.open("meshes/bee.amh");
            beeMesh = new AnimatedMesh(is);
            beeMesh.startAction("bee_armatureAct", true, false);
        } catch (IOException e) {
            Log.e("gl", "Couldn't open bee mesh");
        }
        beeTexture = loadTexture("textures/bee-tex.png");
        speechBubbleTexture = loadTexture("textures/speech-bubble.png");
        int[] buffers = new int[1];
        GLES20.glGenBuffers(1, buffers, 0); // buffer names
        mQuadVertexBuffer = buffers[0];

        font = new GLFont("fonts/komika.png", "fonts/komika.bin");

        mModelview = new float[16];
        Matrix.setIdentityM(mModelview, 0);
        mProjection = new float[16];
        mMVP = new float[16];

        // default shader
        String vertexSource = "attribute vec3 position; void main() {gl_Position = vec4(position, 1.0);}";
        String fragmentSource = "void main() {gl_FragColor = vec4(1.0, 0.0, 1.0, 1.0);}";
        try {
            vertexSource = inputStreamToString(assets.open("shaders/skinned.vert"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fragmentSource = inputStreamToString(assets.open("shaders/skinned.frag"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAnimatedProgram = loadProgram(vertexSource, fragmentSource);

        mTexturedProgram = loadProgram(
                "uniform vec2 resolution;" // vertex
                        + "uniform float scale;"
                        + "attribute vec2 position;"
                        + "attribute vec2 texCoord;"
                        + "varying vec2 vertexTexCoord;"
                        + "void main() {"
                        + "	vertexTexCoord = texCoord;"
                        + "	gl_Position = vec4(scale * (2.0 * (position / resolution) - 1.0), 0.0, 1.0);"
                        + "}",
                "precision mediump float;"
                        + "uniform sampler2D colormap;" // fragment
                        + "uniform vec4 color;"
                        + "varying vec2 vertexTexCoord;"
                        + "void main() {"
                        + " vec4 texel = texture2D(colormap, vertexTexCoord);"
                        + "	gl_FragColor = color * texel;"
                        + "}");
    }

    private void updateAndroidAndBee() {
        // threat level state machine
        if (mTargetThreatLevel != mCurrentThreatLevel) {
            boolean blocked = false; // block until current action is completed
            if (mThreatLevelTransition == 0.0f) {
                if (androidMesh.isActionDone()) {
                    switch (mTargetThreatLevel) {
                        case NOT_MONITORING:
                            androidMesh.startAction("sleep", false, false);
                            break;
                        case NO_THREAT:
                            androidMesh.startAction("happy", true, false);
                            break;
                        case PAST_THREAT:
                            androidMesh.startAction("fear", true, false);
                            break;
                        case LIVE_THREAT:
                            androidMesh.startAction("panic", true, false);
                            break;
                    }
                } else blocked = true;
            }

            if (!blocked) {
                mThreatLevelTransition += 0.016f;
                if (mThreatLevelTransition >= 1.0f) {
                    mCurrentThreatLevel = mTargetThreatLevel;
                    mThreatLevelTransition = 1.0f;
                }
            }
        } else {
            if (mNextThreatLevel != mTargetThreatLevel) {
                mTargetThreatLevel = mNextThreatLevel;
                mThreatLevelTransition = 0.0f;

                // HACK!!! reverses the sleep animation to create smooth transition into other states
                if (mCurrentThreatLevel == ThreatLevel.NOT_MONITORING)
                    androidMesh.startAction("sleep", false, true);
            }
        }

        androidMesh.tick(); // animate android
    }

    private void drawAndroidAndBee(double animTime) {
        GLES20.glUseProgram(mAnimatedProgram);
        int colorUniformLoc = GLES20.glGetUniformLocation(mAnimatedProgram, "color");
        int textureUniformLoc = GLES20.glGetUniformLocation(mAnimatedProgram, "texture");
        int mvpUniformLoc = GLES20.glGetUniformLocation(mAnimatedProgram, "mvp");

        //  animate color
        final float[] whiteColor = {1.0f, 1.0f, 1.0f, 1.0f};
        final float[] greyColor = {0.5f, 0.5f, 0.5f, 1.0f};
        final float[] redColor = {2.0f, 0.4f, 0.2f, 1.0f};
        final float[] yellowColor = {1.1f * 255.0f / 166.0f, 1.2f * 255.0f / 200.0f, 0.0f, 1.0f};

        float[] currentColor = whiteColor;
        float blink = 0.5f + 0.5f * (float) Math.sin(12.0 * animTime);
        switch (mCurrentThreatLevel) {
            case NOT_MONITORING:
                currentColor = greyColor;
                break;
            case PAST_THREAT:
                currentColor = mixColor(blink, whiteColor, yellowColor);
                break;
            case LIVE_THREAT:
                currentColor = mixColor(blink, whiteColor, redColor);
                break;
        }
        if (mTargetThreatLevel != mCurrentThreatLevel) {
            float[] targetColor = whiteColor;
            switch (mTargetThreatLevel) {
                case NOT_MONITORING:
                    targetColor = greyColor;
                    break;
                case PAST_THREAT:
                    targetColor = mixColor(blink, whiteColor, yellowColor);
                    break;
                case LIVE_THREAT:
                    targetColor = mixColor(blink, whiteColor, redColor);
                    break;
            }
            currentColor = mixColor(mThreatLevelTransition, currentColor, targetColor);
        }
        GLES20.glUniform4fv(colorUniformLoc, 1, currentColor, 0);
        GLES20.glUniform1i(textureUniformLoc, 0);

        // animate camera
        Matrix.setIdentityM(mModelview, 0);
        if (mCurrentThreatLevel == ThreatLevel.LIVE_THREAT || mTargetThreatLevel == ThreatLevel.LIVE_THREAT) {
            float delta = 1.0f;
            if (mThreatLevelTransition < 0.4f) { // animate only during the first 40% of the transition
                delta = mThreatLevelTransition / 0.4f;
                delta = -2.0f * delta * delta * delta + 3.0f * delta * delta; // ease in/out
            }
            if (mTargetThreatLevel != ThreatLevel.LIVE_THREAT)
                delta = 1.0f - delta;
            Matrix.translateM(mModelview, 0, 0.0f, -0.6f - 0.2f * delta, -1.6f - 0.4f * delta); // 0.0f, -0.8f, -2.0f
            Matrix.rotateM(mModelview, 0, -85.0f + 5.0f * delta, 1.0f, 0.0f, 0.0f); // -80.0f

        } else {
            Matrix.translateM(mModelview, 0, 0.0f, -0.6f, -1.6f);
            Matrix.rotateM(mModelview, 0, -85.0f, 1.0f, 0.0f, 0.0f);
        }
        Matrix.multiplyMM(mMVP, 0, mProjection, 0, mModelview, 0);
        GLES20.glUniformMatrix4fv(mvpUniformLoc, 1, false, mMVP, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, androidTexture);
        androidMesh.draw(mAnimatedProgram);

        // restore color
        GLES20.glUniform4fv(colorUniformLoc, 1, whiteColor, 0);

        if (mCurrentThreatLevel == ThreatLevel.LIVE_THREAT || mTargetThreatLevel == ThreatLevel.LIVE_THREAT) {
            // draw a bee rotating around the android

            float fadeIn = mThreatLevelTransition;
            if (mTargetThreatLevel != ThreatLevel.LIVE_THREAT) fadeIn = 1.0f - fadeIn; // fade out
            float beePositionZ = 2.0f * (1.0f - fadeIn) * (1.0f - fadeIn); // animate the bee going in/out

            final float beeSize = 0.2f;
            Matrix.rotateM(mModelview, 0, (float) ((-240.0 * animTime) % 360.0), 0.0f, 0.0f, 1.0f); // rotate around android
            Matrix.translateM(mModelview, 0, 0.6f, 0.0f, 0.7f + 0.1f * (float) Math.sin(12.0 * animTime) + beePositionZ); // go up and down
            Matrix.rotateM(mModelview, 0, 20.0f * (float) Math.cos(12.0 * animTime), 1.0f, 0.0f, 0.0f); // rock back and forth
            Matrix.scaleM(mModelview, 0, beeSize, beeSize, beeSize);
            Matrix.multiplyMM(mMVP, 0, mProjection, 0, mModelview, 0);
            GLES20.glUniformMatrix4fv(mvpUniformLoc, 1, false, mMVP, 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, beeTexture);
            beeMesh.tick();
            beeMesh.draw(mAnimatedProgram);
        }
    }

    private float easeInOut(float alpha) {
        return 3.0f * alpha * alpha - 2.0f * alpha * alpha * alpha;
    }

    /**
     * Tries to render at 30 Hz (see bottom part)
     */
    public void onDrawFrame(GL10 arg0) {
        Context ctx = MainActivity.getInstance();

        long timeMillis = System.currentTimeMillis() - mStartTimeMillis;
        double animTime = 0.001 * (double) timeMillis; // in seconds
        float dt = 1.0f / 30.0f;

        // OpenGL drawing
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        updateAndroidAndBee();
        drawAndroidAndBee(animTime);

        if (bubbleWait > 0.0f) {
            bubbleAlpha += 4.0f * dt;
            if (bubbleAlpha >= 1.0f) {
                bubbleAlpha = 1.0f;
                bubbleWait -= dt;
            }
        } else if (bubbleAlpha > 0.0f) {
            bubbleAlpha -= 4.0f * dt;
            if (bubbleAlpha < 0.0f) {
                bubbleAlpha = 0.0f;
            }
        }
        if (bubbleAlpha > 0.0f) {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glUseProgram(mTexturedProgram);
            int resolutionUniformLoc = GLES20.glGetUniformLocation(mTexturedProgram, "resolution");
            int textureUniformLoc = GLES20.glGetUniformLocation(mTexturedProgram, "colormap");
            int colorUniformLoc = GLES20.glGetUniformLocation(mTexturedProgram, "color");
            int scaleUniformLoc = GLES20.glGetUniformLocation(mTexturedProgram, "scale");
            GLES20.glUniform2f(resolutionUniformLoc, 1024.0f, 1024.0f);
            GLES20.glUniform1i(textureUniformLoc, 0);
            GLES20.glUniform4f(colorUniformLoc, 1.0f, 1.0f, 1.0f, 0.8f);
            GLES20.glUniform1f(scaleUniformLoc, easeInOut(bubbleAlpha));
            String message = "???";
            switch (mNextThreatLevel) {
                case NOT_MONITORING:
                    message = ctx.getString(R.string.honeypot_not_monitoring);
                    break;
                case NO_THREAT:
                    message = ctx.getString(R.string.honeypot_no_threat);
                    break;
                case PAST_THREAT:
                    message = ctx.getString(R.string.honeypot_past_threat);
                    break;
                case LIVE_THREAT:
                    message = ctx.getString(R.string.honeypot_live_threat);
                    break;
            }
            float textWidth = font.getTextWidth(message);
            float textHeight = 40.0f;
            float bubbleDiameter = 256.0f;
            float bubbleWidth = textWidth + 0.75f * bubbleDiameter;
            float bubbleHeight = bubbleDiameter;
            float y = 0.8f * 1024.0f + 32.0f * (float) Math.sin(2.0 * animTime);
            float x = 0.5f * 1024.0f + 16.0f * (float) Math.cos(1.0 * animTime);
            drawSpeechBubble(speechBubbleTexture, x - 0.5f * bubbleWidth, y - 0.5f * bubbleHeight,
                    bubbleWidth, bubbleHeight);
            GLES20.glUniform4f(colorUniformLoc, 0.0f, 0.0f, 0.0f, 1.0f);
            font.drawText(mTexturedProgram, message, x - 0.5f * textWidth,
                    y - 0.5f * textHeight);
            GLES20.glUseProgram(0);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDisable(GLES20.GL_BLEND);
        }

        long deltaTime = System.currentTimeMillis() - mStartTimeMillis - timeMillis; // time for one frame
        if (deltaTime < 33) {
            try {
                Thread.sleep(33 - deltaTime); // sleep remaining time for 30 Hz
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Informs renderer of changed surface dimensions
     */
    public void onSurfaceChanged(GL10 arg0, int w, int h) {
        int width = w;
        int height = h;
        float aspectRatio = (float) w / (float) h;
        //Matrix.orthoM(mProjection, 0, -aspectRatio, aspectRatio, -1.0f, 1.0f, -1.0f, 1.0f);
        float near = 0.1f;
        float fov = 2.0f;
        Matrix.frustumM(mProjection, 0, near * -aspectRatio, near * aspectRatio, -near, near, fov * near, 100.0f);
        GLES20.glViewport(0, 0, width, height);
    }

    // some helper functions
    private void drawSpeechBubble(int texture, float x, float y, float w, float h) {
        float bubbleRadius = 128.0f;
        int vertexCount = 8;
        int vertexSize = (2 + 2) * 4; // size in bytes
        FloatBuffer buffer = ByteBuffer.allocateDirect(vertexCount * vertexSize).order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(x);
        buffer.put(y + h);
        buffer.put(0.0f);
        buffer.put(1.0f);
        buffer.put(x);
        buffer.put(y);
        buffer.put(0.0f);
        buffer.put(0.0f);

        buffer.put(x + bubbleRadius);
        buffer.put(y + h);
        buffer.put(0.5f);
        buffer.put(1.0f);
        buffer.put(x + bubbleRadius);
        buffer.put(y);
        buffer.put(0.5f);
        buffer.put(0.0f);

        buffer.put(x + w - bubbleRadius);
        buffer.put(y + h);
        buffer.put(0.5f);
        buffer.put(1.0f);
        buffer.put(x + w - bubbleRadius);
        buffer.put(y);
        buffer.put(0.5f);
        buffer.put(0.0f);

        buffer.put(x + w);
        buffer.put(y + h);
        buffer.put(1.0f);
        buffer.put(1.0f);
        buffer.put(x + w);
        buffer.put(y);
        buffer.put(1.0f);
        buffer.put(0.0f);

        buffer.position(0); // rewind

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mQuadVertexBuffer);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexSize * vertexCount, buffer, GLES20.GL_STREAM_DRAW);

        int positionIndex = GLES20.glGetAttribLocation(mTexturedProgram, "position");
        int texCoordIndex = GLES20.glGetAttribLocation(mTexturedProgram, "texCoord");

        GLES20.glEnableVertexAttribArray(positionIndex);
        GLES20.glEnableVertexAttribArray(texCoordIndex);

        GLES20.glVertexAttribPointer(positionIndex, 2, GLES20.GL_FLOAT, false, vertexSize, 0);
        GLES20.glVertexAttribPointer(texCoordIndex, 2, GLES20.GL_FLOAT, false, vertexSize, 8);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glDisableVertexAttribArray(positionIndex);
        GLES20.glDisableVertexAttribArray(texCoordIndex);
    }

    private float[] mixColor(float alpha, float[] color1, float[] color2) {
        float[] color3 = new float[4];
        color3[0] = (1.0f - alpha) * color1[0] + alpha * color2[0];
        color3[1] = (1.0f - alpha) * color1[1] + alpha * color2[1];
        color3[2] = (1.0f - alpha) * color1[2] + alpha * color2[2];
        color3[3] = (1.0f - alpha) * color1[3] + alpha * color2[3];
        return color3;
    }

    public static int loadTexture(String filePath) {
        AssetManager assets = MainActivity.getInstance().getAssets();

        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(assets.open(filePath));
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        int[] names = {0};
        GLES20.glGenTextures(1, names, 0);
        int tex = names[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle(); // memory is now gpu -> free it

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);

        return tex;
    }

    // see http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
    private static String inputStreamToString(InputStream is) {
        Scanner scanner = new Scanner(is);
        Scanner s = scanner.useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        scanner.close();
        return result;
    }

    private static int loadShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        Log.i("gl", GLES20.glGetShaderInfoLog(shader));
        return shader;
    }

    private static int loadProgram(String vertexSource, String fragmentSource) {
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, loadShader(GLES20.GL_VERTEX_SHADER, vertexSource));
        GLES20.glAttachShader(program, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource));
        GLES20.glLinkProgram(program);
        return program;
    }
}
