package org.pipservices.rpc;

import org.pipservices.commons.data.*;

import com.fasterxml.jackson.annotation.*;

public class Dummy implements IStringIdentifiable {
	private String _id;
	private String _key;
	private String _content;
	private boolean _flag = true;
	
	public Dummy() {}

	public Dummy(String id, String key, String content, boolean flag) {
		_id = id;
		_key = key;
		_content = content;
		_flag = flag;
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

	@JsonProperty("flag")
	public boolean getFlag() {	return _flag;	}
	public void setFlag(boolean _flag) { this._flag = _flag; }	
}
