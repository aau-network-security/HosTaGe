package dk.aau.netsec.hostage.ui.popup;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import dk.aau.netsec.hostage.R;

/**
 * Created by Julien on 13.02.14.
 */
public abstract class AbstractPopup {

    //static final int ORIENTATION_LANDSCAPE = 2;

    /**
     * OnPopupItemClickListener
     * The listener will be called if the user selects a table row
     */
    public interface OnPopupItemClickListener {
        /**
         * Will be called if the user tapped on an item.
         *
         * @param data Object
         */
        void onItemClick(Object data);
    }

    private final PopupWindow popupWindow;
    private final Activity context;
    private final OnPopupItemClickListener onPopupItemClickListener;
    private LinearLayout rootView;
    private final LayoutInflater lInf;
    private View lastItemView;

    /**
     * Override to return the layout id.
     *
     * @return int layoutID
     */
    abstract public int getLayoutId();

    /**
     * Override to make additional stuff with the rootview.
     *
     * @param view rootview
     */
    abstract void configureView(View view);

    /**
     * Constructor
     *
     * @param context  context
     * @param listener listener
     */
    public AbstractPopup(Context context, OnPopupItemClickListener listener) {
        super();
        this.context = (Activity) context;
        onPopupItemClickListener = listener;
        popupWindow = new PopupWindow(context);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        lInf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    /**
     * Override to return a linear layout to add a scrollview.
     *
     * @return LinearLayout
     */
    public abstract LinearLayout getScrollableItemLayout();

    /**
     * Returns the root view
     *
     * @return View the rootview
     */
    public View getRootView() {
        return this.rootView;
    }

    /**
     * Adds a table row item.
     *
     * @param item AbstractPopupItem
     */
    public void addItem(final AbstractPopupItem item) {
        View view = item.getRootView();

        if (rootView == null) {
            rootView = (LinearLayout) lInf.inflate(this.getLayoutId(), null);
            configureView(rootView);
        }
        if (rootView != null) {
            getScrollableItemLayout().addView(view);
            lastItemView = view;

            //this.rootView.addView(view);
            view.setOnTouchListener((view1, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    item.onItemSelect(event);
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_MOVE) {
                    item.onItemDeselect(event);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    item.onItemDeselect(event);
                    AbstractPopup.this.onPopupItemClickListener.onItemClick(item.onClickedResult(event));
                    AbstractPopup.this.popupWindow.dismiss();
                }
                return true;
            });
        }

    }

    /**
     * Returns the rootview.
     * If the root view is null, it initialises it with the layout id.
     *
     * @return View the root view
     */
    public View getPopupView() {
        if (rootView == null) {
            rootView = (LinearLayout) lInf.inflate(getLayoutId(), null);
        }
        return rootView;
    }

    /**
     * Opens the Popup View on top of the given anchor.
     *
     * @param anchorView View
     */
    public void showOnView(final View anchorView) {
        if (rootView == null) {
            rootView = (LinearLayout) lInf.inflate(getLayoutId(), null);
        }
        if (rootView != null) {
            popupWindow.dismiss();

            popupWindow.setContentView(rootView);

            final Rect windowFrame = new Rect();

            Window window = context.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(windowFrame);
            //int orientation = this.context.getResources().getConfiguration().orientation;
            int windowWidth = windowFrame.width();
            int windowHeight = windowFrame.height();

            final int[] position = new int[2];
            anchorView.getLocationOnScreen(position);
            final int anchorWidth = anchorView.getWidth();

            rootView.measure(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            int width = rootView.getMeasuredWidth();
            int height = rootView.getMeasuredHeight();

            //int alh = (position[0] + width) - windowFrame.width();
            //if (alh < 0) alh = 0;
            int offset = windowFrame.top;

            int x = position[0] + (anchorWidth / 2) - (width / 2);
            int y = (position[1] - height) + offset;

            height += (offset / 2);

            width = Math.min(windowWidth, width);

            x = Math.max(0, x);
            x = Math.min(windowWidth - width, x);

            height = (Math.min(windowHeight, height)) - 10;

            y = Math.max(0, y);
            y = Math.min(windowHeight - height, y);

            configureView(rootView);

            int smallBottomOffset = 45;
            popupWindow.setWidth(width);
            popupWindow.setHeight(height);

            if (lastItemView != null) {
                View v = lastItemView.findViewById(R.id.bottom_separator);

                if (v != null) {
                    v.setVisibility(View.GONE);
                }
            }

            popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y - smallBottomOffset);

        }
    }


}
