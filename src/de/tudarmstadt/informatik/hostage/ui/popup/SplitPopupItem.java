package de.tudarmstadt.informatik.hostage.ui.popup;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import de.tudarmstadt.informatik.hostage.R;

/**
 * Created by Julien on 16.02.14.
 */
public class SplitPopupItem extends AbstractPopupItem {

    public final static String LEFT_TITLE = "LEFT_TITLE";
    public final static String RIGHT_TITLE = "RIGHT_TITLE";
    public final static String LEFT_SUBTITLE = "LEFT_SUBTITLE";
    public final static String RIGHT_SUBTITLE = "RIGHT_SUBTITLE";

    public boolean wasRightTouch;

	private Context context;
	private View left_container;
	private View right_container;

    /**
     * Constructor
     *
     * @param Context context
     */
    public SplitPopupItem(Context context){
        super(context);

	    this.context = context;
    }

    @Override
    public int getLayoutId(){
        return R.layout.split_popup_item;
    }

    @Override
    public void configureItemView(View view){
        String leftTitle = (String) this.data.get(LEFT_TITLE);
        String rightTitle = (String) this.data.get(RIGHT_TITLE);

        String leftSubtitle = (String) this.data.get(LEFT_SUBTITLE);
        String rightSubtitle = (String) this.data.get(RIGHT_SUBTITLE);

        TextView leftTitleView = (TextView)view.findViewById(R.id.left_title_text_view);
        leftTitleView.setText(leftTitle);
        TextView leftSubtitleView = (TextView)view.findViewById(R.id.left_subtitle_text_view);
        if (leftSubtitle != null){
            leftSubtitleView.setText(leftSubtitle);
        } else {
            leftSubtitleView.setText("-");
        }

        TextView rightTitleView = (TextView)view.findViewById(R.id.right_title_text_view);
        rightTitleView.setText(rightTitle);
        TextView rightSubtilteView = (TextView)view.findViewById(R.id.right_subtitle_text_view);
        if (rightSubtitle != null){
            rightSubtilteView.setText(rightSubtitle);
        } else {
            rightSubtilteView.setText("-");
        }
    }

    /**
     * Returns the displayed object for the clicked position in the view.
     * E.g. the user tapped the right side, it returns the object representing the right side of the clickt view.
     * @param event MotionEvent
     * @return Object
     */
    public Object onClickedResult(MotionEvent event){
        this.wasRightTouch = isRightTouch(event);
        return this;
    }

    /**
     * Returns true if the user touched the right side of the view.
     * @return boolean isRightTouch
     */
	private boolean isRightTouch(MotionEvent event){
		return event.getX() > this.getRootView().getX() + (this.getRootView().getWidth() / 2);
	}

    /**
     * Returns the left view.
     * @return View the left view
     */
	private View getLeftContainer(){
		if(left_container == null){
			left_container = this.getRootView().findViewById(R.id.popup_left_container);
		}

		return left_container;
	}

    /**
     * Returns the right view.
     * @return View the right view
     */
	private View getRightContainer(){
		if(right_container == null){
			right_container = this.getRootView().findViewById(R.id.popup_right_container);
		}

		return right_container;
	}

    @Override
	public void onItemSelect(MotionEvent event){
		int blue_color = context.getResources().getColor(android.R.color.holo_blue_light);
		int trans_color = context.getResources().getColor(android.R.color.transparent);

		if(!isRightTouch(event)){
			getLeftContainer().setBackgroundColor(blue_color);
			getRightContainer().setBackgroundColor(trans_color);
		} else {
			getLeftContainer().setBackgroundColor(trans_color);
			getRightContainer().setBackgroundColor(blue_color);
		}
	}

    @Override
	public void onItemDeselect(MotionEvent event){
		int trans_color = context.getResources().getColor(android.R.color.transparent);

		getLeftContainer().setBackgroundColor(trans_color);
		getRightContainer().setBackgroundColor(trans_color);
	}
}
