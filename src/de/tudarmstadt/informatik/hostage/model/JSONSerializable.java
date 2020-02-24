package de.tudarmstadt.informatik.hostage.model;

import org.json.JSONObject;

/**
 * @author Alexander Brakowski
 * @created 23.03.14 11:45
 */
public interface JSONSerializable<T> {
	public T fromJSON(JSONObject json);
	public JSONObject toJSON();
}
