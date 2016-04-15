package com.familycircle.sdk.models;

import java.util.List;

/**
 * Created by samratsen on 2/2/15.
 */
public class ContactModel {
    private String firstName;
    private String lastName;
    private String phoneNumber;

    private String avatarUrlSmall;
    private String avatarUrlLarge;
    private String idTag;

    private String contentId;

    public String location;
    public String temperature;
    public String heartRate;
    public String montionType;
    public String speed;
    public String distanceToday;

    private String status="Offline";

    private List<Device> devices;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    private String email;

/*    public void setIdTags(List<String> idTags) {
        this.idTags = idTags;
        if (idTags == null || idTags.isEmpty()) return;
        this.idTag = idTags.get(0);
    }*/

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrlSmall() {
        return avatarUrlSmall;
    }

    public void setAvatarUrlSmall(String avatarUrlSmall) {
        this.avatarUrlSmall = avatarUrlSmall;
    }

    public String getAvatarUrlLarge() {
        return avatarUrlLarge;
    }

    public void setAvatarUrlLarge(String avatarUrlLarge) {
        this.avatarUrlLarge = avatarUrlLarge;
    }

    public void setIdTag(String idTag) {
        this.idTag = idTag;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    public String getIdTag() {

        return idTag;
    }

    public String getName() {
        if (firstName==null && lastName==null){
            return "Unknown";
        }
        return (firstName!=null?firstName:"") + " " + (lastName!=null?lastName:"");
    }
}
