package dk.aau.netsec.hostage.ui.popup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;

/**
 * Created by Julien on 13.02.14.
 */
public abstract class AbstractPopupItem {
    private int itemId;
    private String title;

    private View rootView;

    private LayoutInflater lInf;

    public HashMap<Object, Object> data;

    /**
     * Override to return the layout id.
     * @return int layoutID
     */
    abstract public int getLayoutId();

    /**
     * Override to do additional stuff with the rootview.
     *
     * @param  view the root view
     */
    abstract public void configureItemView(View view);

    /**
     * Set different types of data. Calls {@link #configureItemView(android.view.View)}
     * @param key String
     * @param value Object
     */
    public void setValue(String key, Object value){
        if (key != null && value != null){
            this.data.put(key, value);
            if (this.rootView != null) this.configureItemView(this.rootView);
        }
    }

    /**
     * Add other data to the item.
     * @param map HashMap<Object, Object> optional data.
     */
    public void setMultipleData(HashMap<Object, Object> map){
        if (map != null){
            for(Object key : map.keySet()){
                this.data.put(key, map.get(key));
            }
            if (this.rootView != null) this.configureItemView(this.rootView);
        }
    }

    public void setTitle(String title){
        this.title = title;
        if (this.rootView != null) this.configureItemView(this.rootView);
    }
    public String getTitle(){
        return this.title;
    }

    /**
     * Set a specific item ID to identify it.
     * @param  id int
     */
    public void setItemId(int id){
        this.itemId = id;
        if (this.rootView != null) this.configureItemView(this.rootView);
    }

    /**
     * Returns the item ID.
     * @return int ID
     */
    public int getItemId() {
        return this.itemId;
    }

    /**
     * Constructor
     * @param  context Context
     */
    public AbstractPopupItem(Context context) {
        super();
        this.lInf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.data = new HashMap<Object, Object>();
    }

    /**
     * Returns the rootview.
     * This method calls everytime the ConfigureItemView methode.
     * @return View rootview
     */
    public View getRootView(){
        if (this.rootView == null){
            this.rootView = this.lInf.inflate(this.getLayoutId(), null);
        }
        this.configureItemView(this.rootView);

        return this.rootView;
    }

    /**
     * The method is called if the user clicks the rootview.
     * @param   event MotionEvent
     * @return Object object
     */
    public Object onClickedResult(MotionEvent event){
        return this;
    }

    /**
     * Will be called if the user selected the view.
     * @param  event MotionEvent
     */
	public void onItemSelect(MotionEvent event){
	}

    /**
     * Will be called if the user deselects the view.
     * @param  event MotionEvent
     */
	public void onItemDeselect(MotionEvent event){

	}
}
