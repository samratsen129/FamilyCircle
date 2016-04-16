package com.familycircle.utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.familycircle.utils.network.model.DoorCodeModel;
import com.familycircle.utils.network.model.InviteModel;
import com.familycircle.utils.network.model.UserObject;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;


public class DBInterface {
    public static final String TAG = "ParseUtils";

    public static ParseInstallation getInstallationId(){
        return ParseInstallation.getCurrentInstallation();
    }

    /**
     * Call this only from Application class
     * @param context
     */
    public static void parseInit(Context context){
        String parseAppId = "CijWEq7AJs0Ewc4FIDI0eIW1w2JcmYT0IcarjJp9";
        String parseClientKey = "kzAYTZct2CxDOU3aEVlwBJP9F453G0Cyo9UoeATS";
        Parse.initialize(context, parseAppId, parseClientKey);
        //PushService.startServiceIfRequired(context); // PARSE BUG!!!
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();

        /*Not Required Now
        ParsePush.subscribeInBackground("", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("com.parse.push", "successfully subscribed to the broadcast channel.");

                } else {
                    Log.e("com.parse.push", "failed to subscribe for push", e);
                }
            }
        });*/
    }

    public static ParseObject getUserDeviceObject(final String musername, final String password) throws Exception {

            ParseQuery<ParseObject> query = ParseQuery.getQuery("users");
            query.whereEqualTo("email", musername.trim().toLowerCase());
            String hash = Base64.encodeToString(SecUtil.generateSHA1Hash(password), Base64.NO_WRAP);
            query.whereEqualTo("password", hash);

            Log.d(TAG, "Searching for " + musername + "," + musername);
            List<ParseObject> list = query.find();

            if (list != null && !list.isEmpty()) {
                ParseObject parseObject = list.get(0);
                return parseObject;
            } else {
                return null;
            }

    }

    private static ParseObject getUserDeviceObject2(final String musername, final String familyId) throws Exception {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("users");
        query.whereEqualTo("email", musername.trim().toLowerCase());
        query.whereEqualTo("family_id", familyId);

        Log.d(TAG, "Searching for " + musername + "," + musername);
        List<ParseObject> list = query.find();

        if (list != null && !list.isEmpty()) {
            ParseObject parseObject = list.get(0);
            return parseObject;
        } else {
            return null;
        }

    }

    public static UserObject  getUserDeviceObject(final String musername) throws Exception {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("users");
        query.whereEqualTo("email", musername.trim().toLowerCase());
        List<ParseObject> list = query.find();

        if (list!=null && !list.isEmpty()){
            UserObject userDevice = new UserObject();
            ParseObject parseObject = list.get(0);
            userDevice.name = parseObject.getString("name");
            userDevice.family_id = parseObject.getString("family_id");
            userDevice.email = parseObject.getString("email").toLowerCase();
            userDevice.contact_number = parseObject.getString("contact_number");
            userDevice.device_id= parseObject.getString("device_id");
            userDevice.objectId= parseObject.getString("objectId");
            userDevice.is_head= parseObject.getBoolean("is_head");
            userDevice.m2x_id = parseObject.getString("m2x_id");
            userDevice.password = parseObject.getString("password");

            return userDevice;

        } else {
            return null;

        }
    }

    public static UserObject getUserDevice(final String musername, final String familyId) throws Exception {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("users");
        query.whereEqualTo("email", musername.trim().toLowerCase());
        query.whereEqualTo("family_id", familyId);
        List<ParseObject> list = query.find();

        if (list!=null && !list.isEmpty()){
            UserObject userDevice = new UserObject();
            ParseObject parseObject = list.get(0);
            userDevice.name = parseObject.getString("name");
            userDevice.family_id = familyId;
            userDevice.email = parseObject.getString("email").toLowerCase();
            userDevice.contact_number = parseObject.getString("contact_number");
            userDevice.device_id= parseObject.getString("device_id");
            userDevice.objectId= parseObject.getString("objectId");
            userDevice.is_head= parseObject.getBoolean("is_head");
            userDevice.m2x_id = parseObject.getString("m2x_id");
            userDevice.password = parseObject.getString("password");

            return userDevice;

        } else {
            return null;

        }

    }

    public static boolean insertUserDevice(final UserObject userDevice, boolean checkBeforeInsert) throws Exception {

        if (checkBeforeInsert){
            if (getUserDeviceObject(userDevice.email, userDevice.password)!=null){
                return false;
            }
        }

        ParseObject userDeviceNew = new ParseObject("users");
        userDeviceNew.put("name", userDevice.name);
        userDeviceNew.put("family_id", userDevice.family_id);
        final String hash = Base64.encodeToString(SecUtil.generateSHA1Hash(userDevice.password), Base64.NO_WRAP);
        userDeviceNew.put("password", hash);
        userDeviceNew.put("email", userDevice.email.toLowerCase());
        userDeviceNew.put("contact_number", userDevice.contact_number);
        userDeviceNew.put("device_id", userDevice.device_id);
        //userDeviceNew.put("objectId", userDevice.objectId);
        userDeviceNew.put("is_head", userDevice.is_head);
        userDeviceNew.put("m2x_id", userDevice.m2x_id);

        userDeviceNew.save();

        return true;
    }

    public static boolean updateUserDevice(final UserObject userDevice) throws Exception {

        ParseObject userDeviceNew = getUserDeviceObject2(userDevice.email.toLowerCase(), userDevice.family_id);

        if (userDeviceNew==null){
            // if its not there, insert a new record
            Log.d(TAG, "updateUserDevice not found " + userDevice.email.toLowerCase() + ", " + userDevice.family_id);
            return false;
        }

        Log.d(TAG, "updateUserDevice found updating " + userDevice.email.toLowerCase() + ", " + userDevice.family_id);

        userDeviceNew.put("name", userDevice.name);
        userDeviceNew.put("family_id", userDevice.family_id);
        final String hash = Base64.encodeToString(SecUtil.generateSHA1Hash(userDevice.password), Base64.NO_WRAP);

        String currentHash = userDeviceNew.getString("password");
        if (!currentHash.equals(hash)){
            Log.e(TAG, "user credentials not matching " + userDevice.email.toLowerCase() + ", " + userDevice.family_id);
            return false;
        }

        userDeviceNew.put("password", hash);
        userDeviceNew.put("email", userDevice.email.toLowerCase());
        userDeviceNew.put("contact_number", userDevice.contact_number);
        userDeviceNew.put("device_id", userDevice.device_id);
        //userDeviceNew.put("objectId", userDevice.objectId);
        userDeviceNew.put("is_head", userDevice.is_head);
        userDeviceNew.put("m2x_id", userDevice.m2x_id);

        userDeviceNew.save();

        return true;

    }

    public static boolean removeAccount(final String musername, final String familyId) throws Exception {

        ParseObject userDeviceNew = getUserDeviceObject2(musername.toLowerCase(), familyId);
        if (userDeviceNew!=null) {
            userDeviceNew.delete();
            return true;
        }

        return false;
    }

    public static InviteModel getUserInvited(final String musername) throws Exception {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("invites");
        query.whereEqualTo("to_user", musername.trim().toLowerCase());
        List<ParseObject> list = query.find();

        if (list!=null && !list.isEmpty()){
            InviteModel userDevice = new InviteModel();
            ParseObject parseObject = list.get(0);
            userDevice.fromUser = parseObject.getString("from_user");
            userDevice.toUser = parseObject.getString("to_user");
            userDevice.familyId = parseObject.getString("family_id");
            userDevice.is_pending = parseObject.getBoolean("is_pending");
            userDevice.invite_code = parseObject.getString("invite_code");

            return userDevice;

        } else {
            return null;

        }
    }

    public static  boolean insertUserDevice(final InviteModel userDevice, boolean checkBeforeInsert) throws Exception {

        if (checkBeforeInsert){
            if (getUserDeviceObject(userDevice.toUser)!=null){
                return false;
            }
        }

        ParseObject userDeviceNew = new ParseObject("invites");
        userDeviceNew.put("to_user", userDevice.toUser);
        userDeviceNew.put("from_user", userDevice.fromUser);
        userDeviceNew.put("is_pending", userDevice.is_pending);
        userDeviceNew.put("family_id", userDevice.familyId);
        userDeviceNew.put("invite_code", userDevice.invite_code);
        userDeviceNew.save();

        return true;
    }

    public static boolean updateUserDevice(final InviteModel userDevices) throws Exception {

        UserObject userDeviceNew = getUserDeviceObject(userDevices.toUser.toLowerCase());

        if (userDeviceNew==null){
            // if its not there, insert a new record
            Log.d(TAG, "updateUserDevice not found " + userDevices.toUser.toLowerCase() );
            return false;
        }

        Log.d(TAG, "updateUserDevice found updating " + userDevices.toUser.toLowerCase());

        ParseObject userDevice = new ParseObject("invites");
        userDevice.put("to_user", userDevices.toUser);
        userDevice.put("from_user", userDevices.fromUser);
        userDevice.put("is_pending", userDevices.is_pending);
        userDevice.put("family_id", userDevices.familyId);
        userDevice.put("invite_code", userDevices.invite_code);

        userDevice.save();

        return true;

    }

    public static DoorCodeModel getDoorCode(final String code) throws Exception {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("door_code");
        query.whereEqualTo("code", code);
        List<ParseObject> list = query.find();

        if (list!=null && !list.isEmpty()){
            DoorCodeModel doorCodeModel = new DoorCodeModel();
            ParseObject parseObject = list.get(0);
            doorCodeModel.fromUser = parseObject.getString("from_user");
            doorCodeModel.toUser = parseObject.getString("to_user");
            doorCodeModel.code = parseObject.getString("code");

            return doorCodeModel;

        } else {
            return null;

        }
    }

    public static  boolean insertDoorCode(final DoorCodeModel doorCodeModel) throws Exception {


        ParseObject userDeviceNew = new ParseObject("door_code");
        userDeviceNew.put("to_user", doorCodeModel.toUser);
        userDeviceNew.put("from_user", doorCodeModel.fromUser);
        userDeviceNew.put("code", doorCodeModel.code);
        userDeviceNew.save();

        return true;
    }

}