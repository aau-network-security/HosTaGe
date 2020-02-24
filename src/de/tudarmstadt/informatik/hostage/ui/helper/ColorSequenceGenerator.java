package de.tudarmstadt.informatik.hostage.ui.helper;

import android.graphics.Color;

/**
 * Idea from http://ridiculousfish.com/blog/posts/colors.html
 * Created by Fabio Arnold on 25.02.14.
 */
public class ColorSequenceGenerator {
	private static final int BIT_COUNT = 30; // sadly there is no unsigned type in java
	public static int getColorForIndex(int index) {
		int reverseIndex = 0;
		for (int i = 0; i  < BIT_COUNT; i++) {
			reverseIndex = (reverseIndex << 1) | (index & 1);
			index >>= 1;
		}
		float hue = ((float)reverseIndex / (float)(1 << BIT_COUNT) + 0.0f) % 1.0f;

		float[] hsv = new float[3];
		hsv[0] = 360.0f * hue;
		hsv[1] = 0.7f; // not fully saturated
		hsv[2] = 0.9f;

		return Color.HSVToColor(hsv);
	}
}
