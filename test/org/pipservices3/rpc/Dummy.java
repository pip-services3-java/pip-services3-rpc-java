package org.pipservices3.rpc;

import org.pipservices3.commons.data.*;

import com.fasterxml.jackson.annotation.*;

import java.util.ArrayList;
import java.util.List;

public class Dummy implements IStringIdentifiable {
	private String _id;
	private String _key;
	private String _content;
	private List<SubDummy> _array = new ArrayList<>();
	
	public Dummy() {}

	public Dummy(String id, String key, String content, List<SubDummy> array) {
		_id = id;
		_key = key;
		_content = content;
		_array = array;
	}
	
	@JsonProperty("id")
	public String getId() { return _id; }
	public void setId(String value) { _id = value; }
	
	@JsonProperty("key")
	public String getKey() { return _key; }
	public void setKey(String value) { _key = value; }
	
	@JsonProperty("content")
	public String getContent() { return _content; }
	public void setContent(String value) { _content = value; }

	@JsonProperty("array")
	public List<SubDummy> getArray() {	return _array;	}
	public void setArray(List<SubDummy> value) { this._array = value; }
}
