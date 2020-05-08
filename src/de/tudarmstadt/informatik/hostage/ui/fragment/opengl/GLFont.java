package de.tudarmstadt.informatik.hostage.ui.fragment.opengl;

import android.content.res.AssetManager;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

/**
 * Created by Fabio Arnold on 13.02.15.
 */
public class GLFont {
	private class GlyphMetric {
		float x0,y0,s0,t0; // top-left
		float x1,y1,s1,t1; // bottom-right
		float advance;
	}

	private int mTexture;
	private int mVertexBuffer;
	private GlyphMetric[] mMetrics;

	public GLFont(String texFilePath, String metricsFilePath) {
		AssetManager assets = MainActivity.getInstance().getAssets();

		mTexture = ThreatIndicatorGLRenderer.loadTexture(texFilePath);

		InputStream is = null;
		try {
			is = assets.open(metricsFilePath);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		//DataInputStream dis = new DataInputStream(is);
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

		mMetrics = new GlyphMetric[96];
		for (int i = 0; i < 96; i++) {
			mMetrics[i] = new GlyphMetric();

			mMetrics[i].x0 = data.getFloat();
			mMetrics[i].y0 = data.getFloat();
			mMetrics[i].s0 = data.getFloat();
			mMetrics[i].t0 = data.getFloat();
			mMetrics[i].x1 = data.getFloat();
			mMetrics[i].y1 = data.getFloat();
			mMetrics[i].s1 = data.getFloat();
			mMetrics[i].t1 = data.getFloat();
			mMetrics[i].advance = data.getFloat();
		}

		int[] buffers = new int[1];
		GLES20.glGenBuffers(1, buffers, 0); // buffer names
		mVertexBuffer = buffers[0];
	}

	public float getTextWidth(String text) {
		float x = 0.0f;
		for (int i = 0; i < text.length(); i++) {
			x += mMetrics[(int)text.charAt(i)-32].advance;
		}
		return x;
	}

	public void drawText(int program, String text, float x, float y) {
		int vertexCount = text.length() * 6;
		int vertexSize = (2+2)*4; // size in bytes

		FloatBuffer buffer = ByteBuffer.allocateDirect(vertexCount * vertexSize).order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < text.length(); i++) {
			int glyph = ((int)text.charAt(i))-32;
			GlyphMetric metric = mMetrics[glyph];

			buffer.put(x+metric.x0); buffer.put(y+metric.y0);
			buffer.put(metric.s0); buffer.put(metric.t1);
			buffer.put(x+metric.x1); buffer.put(y+metric.y0);
			buffer.put(metric.s1); buffer.put(metric.t1);
			buffer.put(x+metric.x1); buffer.put(y+metric.y1);
			buffer.put(metric.s1); buffer.put(metric.t0);

			buffer.put(x+metric.x0); buffer.put(y+metric.y0);
			buffer.put(metric.s0); buffer.put(metric.t1);
			buffer.put(x+metric.x1); buffer.put(y+metric.y1);
			buffer.put(metric.s1); buffer.put(metric.t0);
			buffer.put(x+metric.x0); buffer.put(y+metric.y1);
			buffer.put(metric.s0); buffer.put(metric.t0);

			x += metric.advance;
		}
		buffer.position(0); // rewind

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBuffer);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexSize * vertexCount, buffer, GLES20.GL_STREAM_DRAW);

		int positionIndex = GLES20.glGetAttribLocation(program, "position");
		int texCoordIndex = GLES20.glGetAttribLocation(program, "texCoord");

		GLES20.glEnableVertexAttribArray(positionIndex);
		GLES20.glEnableVertexAttribArray(texCoordIndex);

		GLES20.glVertexAttribPointer(positionIndex, 2, GLES20.GL_FLOAT, false, vertexSize, 0);
		GLES20.glVertexAttribPointer(texCoordIndex, 2, GLES20.GL_FLOAT, false, vertexSize, 8);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		GLES20.glDisableVertexAttribArray(positionIndex);
		GLES20.glDisableVertexAttribArray(texCoordIndex);
	}
}
