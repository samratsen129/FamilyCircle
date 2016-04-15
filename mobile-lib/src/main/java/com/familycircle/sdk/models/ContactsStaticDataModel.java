package com.familycircle.sdk.models;

import android.util.Log;

import com.familycircle.lib.utils.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by samratsen on 2/5/15.
 */
public class ContactsStaticDataModel
{
    //private static List<ContactModel> contacts = new ArrayList<ContactModel>();//active contacts

    private static List<ContactModel> allContacts = new ArrayList<ContactModel>();//active contacts

    private static ContactModel logInUser = new ContactModel();

    public static ContactModel getLogInUser(){
        if (allContacts!=null && logInUser.getName().equalsIgnoreCase("Unknown")){
            for (ContactModel contactModel: allContacts) {
                if (contactModel.getIdTag().equalsIgnoreCase(logInUser.getIdTag())) {
                    logInUser = contactModel;
                    break;
                }
            }
        }
        return logInUser;
    }

    public static void setLogInUser(ContactModel contactModel){
        logInUser = contactModel;
    }

/*    static {
        allContacts = new ArrayList<ContactModel>();

        ContactModel contactModel = new ContactModel();
        contactModel.setIdTag("ssen@mportal.com");
        contactModel.setFirstName("samrat");
        contactModel.setLastName("sen");
        contactModel.setPhoneNumber("111-111-1111");
        contactModel.setStatus("Online");
        allContacts.add(contactModel);

        ContactModel contactModel1 = new ContactModel();
        contactModel1.setIdTag("samrat@mportal.com");
        contactModel1.setFirstName("samrat");
        contactModel1.setPhoneNumber("111-111-1111");
        allContacts.add(contactModel1);

        ContactModel contactModel2 = new ContactModel();
        contactModel2.setIdTag("jdoe@mportal.com");
        contactModel2.setFirstName("john");
        contactModel2.setPhoneNumber("111-111-1111");
        contactModel.setStatus("Online");
        allContacts.add(contactModel2);

        ContactModel contactModel3 = new ContactModel();
        contactModel3.setIdTag("sally@mportal.com");
        contactModel3.setFirstName("sal");
        contactModel3.setPhoneNumber("111-111-1111");
        allContacts.add(contactModel3);

    }*/

    public synchronized static List<ContactModel> getContacts(){
        return allContacts;
    }

    public synchronized static void clearContacts(){
        if (allContacts!=null) allContacts.clear();
    }

    public synchronized static List<ContactModel> getAllContacts(){


            try {
                Collections.sort(allContacts, new Comparator<ContactModel>() {
                    public int compare(ContactModel result1, ContactModel result2) {
                        return result2.getStatus().compareTo(result1.getStatus());
                    }
                });

            } catch (Exception e){
                e.printStackTrace();
                Logger.e("Error while sorting Contacts", e);
            }

            return allContacts;

    }

    public synchronized static List<ContactModel> getAllUniqueContacts(){

        List<ContactModel> contactList = new ArrayList<ContactModel>();
        //contactList.addAll(getAllContacts());

        if (allContacts!=null){
            for (ContactModel contactModel: allContacts){
                if (!contactModel.getIdTag().trim().equalsIgnoreCase(logInUser.getIdTag().trim())){
                     contactList.add(contactModel);
                }
            }
        }
        return contactList;
    }

    public synchronized static List<ContactModel> getAllActiveContacts(){

        List<ContactModel> contactList = new ArrayList<ContactModel>();
        if (allContacts!=null) {
            for (ContactModel contactModel : allContacts) {
                if (!contactModel.getIdTag().equalsIgnoreCase(logInUser.getIdTag())
                        && contactModel.getStatus().equalsIgnoreCase("online")) {
                    contactList.add(contactModel);
                }
            }
        }
        return contactList;
    }

    public synchronized  static void setAllContacts(List<ContactModel> contacts){
        allContacts = contacts;

    }

    public synchronized static void addContact(ContactModel model){
        if (model==null||model.getIdTag()==null||model.getIdTag().isEmpty()) return;
        //if (logInUser!=null && logInUser.getIdTag()!=null && model.getIdTag().trim().equalsIgnoreCase(logInUser.getIdTag().trim())){
        //    return;
        //}



        if (allContacts!=null) {
            boolean found = false;
            for (ContactModel contactModel : allContacts) {
                if (contactModel.getIdTag().equalsIgnoreCase(model.getIdTag().trim())) {
                    contactModel.setStatus(model.getStatus());
                    found = true;
                    break;
                }
            }

            if (!found) {
                allContacts.add(model);
            }
        }

    }

    public synchronized static ContactModel getContactByIdTag(String idTag){
        if (allContacts==null||idTag==null) return null;
        for (ContactModel contactModel: allContacts){
            if (contactModel.getIdTag().equalsIgnoreCase(idTag.trim())){
                return contactModel;
            }
        }
        return null;
    }
}
