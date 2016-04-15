package com.familycircle.sdk.models;

/**
 * Created by samratsen on 5/28/15.
 */
public class ChannelConfig {

    private String apiKey;
    private int connectionRetries=10;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    private String groupId; // domain id e.g mportal.com

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getConnectionRetries() {
        return connectionRetries;
    }

    public void setConnectionRetries(int connectionRetries) {
        this.connectionRetries = connectionRetries;
    }


}
