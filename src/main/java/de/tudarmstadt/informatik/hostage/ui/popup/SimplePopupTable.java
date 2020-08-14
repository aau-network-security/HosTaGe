package de.tudarmstadt.informatik.hostage.ui.popup;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.tudarmstadt.informatik.hostage.R;

/**
 * Created by Julien on 13.02.14.
 */
public class SimplePopupTable extends AbstractPopup {

    private String title;

    /**
     * Set the popup title.
     * @param title string
     */
    public void setTitle(String title){
        this.title = title;
        if (this.getPopupView() != null) this.configureView(this.getPopupView());
    }

    /**
     * Returns the popup title.
     * @return String title.
     */
    public String getTitle(){
        return this.title;
    }

    /**
     * Constructor
     * @param context the context
     * @param listener user event listener
     */
    public SimplePopupTable(Context context, OnPopupItemClickListener listener){
        super(context, listener);
    }

    @Override
    public LinearLayout getScrollableItemLayout() {
        return (LinearLayout) this.getRootView().findViewById(R.id.item_scroll_layout);
    }

    @Override
    public int getLayoutId(){
        return R.layout.simple_popup_table;
    }

    @Override
    void configureView(View view){
        TextView titleView = view.findViewById(R.id.title_text_view);
        titleView.setText(this.title);
    }
}
