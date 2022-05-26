package org.pipservices3.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubDummy {
    public SubDummy(String key, String content) {
        this.key = key;
        this.content = content;
    }

    public SubDummy() {}

    private String key;

    @JsonProperty("key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String content;

    @JsonProperty("content")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
