package com.familycircle.sdk.models;

/**
 * Created by samratsen on 4/22/15.
 */
public class RecordingsModel {
    private int id;
    private long size;
    private String name;
    private String fileName;
    private String description;
    private String modifyDate;

    public int getId(){ return id; }

    public void setId(int val){ id = val;}

    public long getSize(){ return size; }

    public void setSize(long val){ size = val;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(String modifyDate) {
        this.modifyDate = modifyDate;
    }


}
