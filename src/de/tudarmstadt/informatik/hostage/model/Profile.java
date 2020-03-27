package de.tudarmstadt.informatik.hostage.model;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.informatik.hostage.ui.activity.MainActivity;

/**
 * @author Alexander Brakowski
 * @created 14.01.14 18:04
 */
public class Profile implements JSONSerializable<Profile> {
	public String mText;
	public String mLabel;
	public int mId;
	public boolean mActivated;
	transient public Bitmap mIcon;
	transient public int mIconId;
	public String mIconName;
	public String mIconPath;

	public boolean mEditable = false;
	public boolean mIsRandom = false;

	public HashMap<String, Boolean> mActiveProtocols = new HashMap<String, Boolean>();
	public String mGhostPorts = "";
	public boolean mGhostActive = false;
	public boolean mShowTooltip = false;

	public Profile(){
		this.mEditable = true;
		this.mActivated = false;
		this.mId = -1;
	}

	public Profile(int id, String label, String text, Bitmap icon, boolean editable){
		this.mId = id;
		this.mLabel = label;
		this.mText = text;
		this.mActivated = false;
		this.mIcon = icon;
		this.mEditable = editable;
	}

	public Profile(int id, String label, String text, int icon, boolean editable){
		this(id, text, label, BitmapFactory.decodeResource(MainActivity.context.getResources(), icon), editable);
		mIconId = icon;
		mIconName = MainActivity.context.getResources().getResourceName(icon);
	}

	public Profile(int id, String label, String text, String iconPath, boolean editable){
		this.mId = id;
		this.mLabel = label;
		this.mText = text;
		this.mActivated = false;
		this.mIconPath = iconPath;
		this.mEditable = editable;
	}

	public Profile(Parcel in) {
		mText = in.readString();
		mLabel = in.readString();
		mId = in.readInt();
		mActivated = in.readInt() == 1;
		mIconId = in.readInt();
		mIconName = in.readString();
		mIconPath = in.readString();
		mEditable = in.readInt() == 1;
		mActiveProtocols = (HashMap<String,Boolean>) in.readSerializable();


	}

	public void setIcon(Bitmap bitmap){
		this.mIcon = bitmap;
	}

	public void setIcon(int icon){
		this.mIcon = BitmapFactory.decodeResource(MainActivity.context.getResources(), icon);
	}

	public Bitmap getIconBitmap(){
		if(this.mIcon != null) return mIcon;

		if(this.mIconId != 0){
			this.mIcon = BitmapFactory.decodeResource(MainActivity.context.getResources(), mIconId);
			return this.mIcon;
		}

		if(this.mIconName != null && !this.mIconName.isEmpty()){
			this.mIconId = MainActivity.context.getResources().getIdentifier(this.mIconName,
					"drawable", "de.tudarmstadt.informatik.hostage");
			this.mIcon = BitmapFactory.decodeResource(MainActivity.context.getResources(), this.mIconId);

			return this.mIcon;
		}

		if(this.mIconPath != null){
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			Bitmap bitmap = BitmapFactory.decodeFile(this.mIconPath, options);

			return bitmap;
		}

		return null;
	}

	public boolean isProtocolActive(String protocol){
		if(!mActiveProtocols.containsKey(protocol)) return false;
		return mActiveProtocols.get(protocol);
	}

	public List<String> getActiveProtocols(){
		List<String> list = new LinkedList<String>();
		for(Map.Entry<String, Boolean> entry: this.mActiveProtocols.entrySet()){
			if(entry.getValue()){
				list.add(entry.getKey());
			}
		}

		return list;
	}

	public Drawable getIconDrawable(){
		return new BitmapDrawable(MainActivity.context.getResources(), getIconBitmap());
	}

	public boolean isEditable(){
		return this.mEditable;
	}

	public Profile cloneProfile(){
		return new Profile(mId, mLabel, mText, mIcon, mEditable);
	}

	public Integer[] getGhostPorts(){
		String[] splits = this.mGhostPorts.split(",");
		Integer[] ports = new Integer[splits.length];

		for(int i=0; i<splits.length; i++){
            if(!splits[i].equals("")) {
                ports[i] = Integer.valueOf(splits[i]);
            }
		}

		return ports;
	}

	public JSONObject toJSON(){
		JSONObject jsonObj = new JSONObject();

		try {
			jsonObj.put("text", mText);
			jsonObj.put("label", mLabel);
			jsonObj.put("id", mId);
			jsonObj.put("activated", mActivated);
			jsonObj.put("icon_name", mIconName);
			jsonObj.put("icon_path", mIconPath);
			jsonObj.put("editable", mEditable);
			jsonObj.put("random", mIsRandom);
			jsonObj.put("ghost_active", mGhostActive);
			jsonObj.put("ghost_ports", mGhostPorts);
			jsonObj.put("active_protocols", new JSONObject(mActiveProtocols));
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return jsonObj;
	}

	public Profile fromJSON(JSONObject json){
		mText = json.optString("text", "");
		mLabel = json.optString("label", "");
		mId = json.optInt("id", -1);
		mActivated = json.optBoolean("activated", false);
		mIconName = json.optString("icon_name", null);
		mIconPath = json.optString("icon_path", null);
		mEditable = json.optBoolean("editable", true);
		mIsRandom = json.optBoolean("random", false);
		mGhostActive = json.optBoolean("ghost_active", false);
		mGhostPorts = json.optString("ghost_ports", "");

		JSONObject activeProtocols = json.optJSONObject("active_protocols");
		if(activeProtocols != null){
			Iterator keys = activeProtocols.keys();

			while(keys.hasNext()){
				String protocol = (String) keys.next();
				try {
					mActiveProtocols.put(protocol, activeProtocols.getBoolean(protocol));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}

		return this;
	}
}