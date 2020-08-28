package dk.aau.netsec.hostage.ui.popup;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import dk.aau.netsec.hostage.R;


/**
 * Created by Julien on 13.02.14.
 */
public class SimplePopupItem extends AbstractPopupItem {

    public boolean selected;
	private Context context;
	private View container;

    /**
     * Constructor
     * @param context the context
     */
	public SimplePopupItem(Context context) {
        super(context);

	    this.context = context;
    }

    @Override
    public int getLayoutId(){
        return R.layout.simple_popup_item;
    }

    @Override
    public void configureItemView(View view){
        TextView titleView = view.findViewById(R.id.title_text_view);
        RadioButton cbox = view.findViewById(R.id.isSelectedButton);
        titleView.setText(this.getTitle());

        if (this.isSelected()){
            cbox.setVisibility(View.VISIBLE);
        } else {
            cbox.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Set the selection state.
     * @param selected boolean
     */
    public void setSelected(boolean selected){
        this.selected = selected;
        if (this.getRootView() != null) this.configureItemView(this.getRootView());
    }

    /**
     * Return the background view.
     * @return view the background view
     */
	private View getContainer(){
		if(container == null){
			container = this.getRootView().findViewById(R.id.popup_item_container);
		}

		return container;
	}

    /**
     * Returns true if the item is selected, otherwise false.
     * @return boolean
     */
    public boolean isSelected(){
        return this.selected;
    }

    @Override
	public void onItemSelect(MotionEvent event){
		getContainer().setBackgroundColor(
				context.getResources().getColor(android.R.color.holo_blue_light));
	}
    @Override
	public void onItemDeselect(MotionEvent event){
		getContainer().setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
	}
}
