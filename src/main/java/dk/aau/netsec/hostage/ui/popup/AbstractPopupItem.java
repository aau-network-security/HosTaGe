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

    private final LayoutInflater lInf;

    public final HashMap<Object, Object> data;

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
            data.put(key, value);
            if (rootView != null) this.configureItemView(rootView);
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
            if (rootView != null) this.configureItemView(rootView);
        }
    }

    public void setTitle(String title){
        title = title;
        if (rootView != null) this.configureItemView(rootView);
    }
    public String getTitle(){
        return title;
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
        lInf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        data = new HashMap<>();
    }

    /**
     * Returns the rootview.
     * This method calls everytime the ConfigureItemView methode.
     * @return View rootview
     */
    public View getRootView(){
        if (rootView == null){
            rootView = lInf.inflate(getLayoutId(), null);
        }
        configureItemView(rootView);

        return rootView;
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
