package de.tudarmstadt.informatik.hostage.ui.swipelist;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Extends the SwipeListView with an mechanism to allow to open only one item at the same time.
 *
 * @author Alexander Brakowski
 * @created 28.02.14 22:05
 */
public class SwipeListView extends com.fortysevendeg.android.swipelistview.SwipeListView {

	/**
	 * {@inheritDoc}
	 */
	public SwipeListView(Context context, int swipeBackView, int swipeFrontView) {
		super(context, swipeBackView, swipeFrontView);
	}

	/**
	 * {@inheritDoc}
	 */
	public SwipeListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * {@inheritDoc}
	 */
	public SwipeListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onOpened(int position, boolean toRight) {
		super.onOpened(position, toRight);

		int start = getFirstVisiblePosition();
		int end = getLastVisiblePosition();

		// close all visible items other then the current one
		for(int i=start; i<=end; i++){
			if(i != position){
				closeAnimate(i);
			}
		}
	}
}
