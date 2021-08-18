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

    private PopupWindow popupWindow;
    private Activity context;
    private OnPopupItemClickListener onPopupItemClickListener;
    private LinearLayout rootView;
    private LayoutInflater lInf;
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
        this.onPopupItemClickListener = listener;
        this.popupWindow = new PopupWindow(context);
        this.popupWindow.setOutsideTouchable(true);
        this.popupWindow.setFocusable(true);
        this.lInf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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

        if (this.rootView == null) {
            this.rootView = (LinearLayout) this.lInf.inflate(this.getLayoutId(), null);
            this.configureView(this.rootView);
        }
        if (this.rootView != null) {
            this.getScrollableItemLayout().addView(view);
            lastItemView = view;

            //this.rootView.addView(view);
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
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
                }
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
        if (this.rootView == null) {
            this.rootView = (LinearLayout) this.lInf.inflate(this.getLayoutId(), null);
        }
        return this.rootView;
    }

    /**
     * Opens the Popup View on top of the given anchor.
     *
     * @param anchorView View
     */
    public void showOnView(final View anchorView) {
        if (this.rootView == null) {
            this.rootView = (LinearLayout) this.lInf.inflate(this.getLayoutId(), null);
        }
        if (this.rootView != null) {
            AbstractPopup.this.popupWindow.dismiss();

            this.popupWindow.setContentView(this.rootView);

            final Rect windowFrame = new Rect();

            Window window = this.context.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(windowFrame);
            //int orientation = this.context.getResources().getConfiguration().orientation;
            int windowWidth = windowFrame.width();
            int windowHeight = windowFrame.height();

            final int[] position = new int[2];
            anchorView.getLocationOnScreen(position);
            final int anchorWidth = anchorView.getWidth();

            this.rootView.measure(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            int width = this.rootView.getMeasuredWidth();
            int height = this.rootView.getMeasuredHeight();

            //int alh = (position[0] + width) - windowFrame.width();
            //if (alh < 0) alh = 0;
            int offset = windowFrame.top;

            int x = position[0] + (anchorWidth / 2) - (width / 2);
            int y = (position[1] - height) + offset;

            height += (offset / 2);

            width = windowWidth < width ? windowWidth : width;

            x = Math.max(0, x);
            x = Math.min(windowWidth - width, x);

            height = (windowHeight < height ? windowHeight : height) - 10;

            y = Math.max(0, y);
            y = Math.min(windowHeight - height, y);

            AbstractPopup.this.configureView(this.rootView);

            int smallBottomOffset = 45;
            this.popupWindow.setWidth(width);
            this.popupWindow.setHeight(height);

            if (lastItemView != null) {
                View v = lastItemView.findViewById(R.id.bottom_separator);

                if (v != null) {
                    v.setVisibility(View.GONE);
                }
            }

            this.popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y - smallBottomOffset);

        }
    }


}
