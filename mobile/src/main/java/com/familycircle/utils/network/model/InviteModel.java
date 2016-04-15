package com.familycircle.utils.network.model;

/**
 * Created by samratsen on 4/12/16.
 */
public class InviteModel extends BaseModel {
    public String fromUser;
    public String toUser;
    public String familyId;
    public boolean is_pending = false;
    public String invite_code;
}
