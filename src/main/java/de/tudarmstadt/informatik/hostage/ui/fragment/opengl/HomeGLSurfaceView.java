package de.tudarmstadt.informatik.hostage.ui.fragment.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * the gl surface view used in the homefragment layout
 * creates the threat indicator renderer
 */
public class HomeGLSurfaceView extends GLSurfaceView {
	public HomeGLSurfaceView(Context context) { // won't be called
		super(context);
		Log.e("gl", "called wrong constructor (w/o attributes)");
	}
	
	// this constructor will be called
	public HomeGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setEGLContextClientVersion(2); // OpenGL ES 2.0
		// setZOrderOnTop(true);
		// transparency
		// setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		// getHolder().setFormat(PixelFormat.RGBA_8888);
		setRenderer(new ThreatIndicatorGLRenderer());
	}
	
	// TODO: just for testing -> remove this eventually
	/*@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			ThreatIndicatorGLRenderer.ThreatLevel threatLevel = ThreatIndicatorGLRenderer.ThreatLevel.NO_THREAT;
			if (event.getX() > 0.5f * getWidth()) threatLevel = ThreatIndicatorGLRenderer.ThreatLevel.PAST_THREAT;
			if (event.getY() > 0.5f * getHeight()) threatLevel = ThreatIndicatorGLRenderer.ThreatLevel.LIVE_THREAT;
			ThreatIndicatorGLRenderer.setThreatLevel(threatLevel);
		}
		return false;
	}*/
}
