package com.familycircle.utils.network.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samratsen on 4/15/16.
 */
public class M2XTriggerModel extends BaseModel {

    public List<Trigger> triggerList = new ArrayList<Trigger>();

    public static class Trigger {
        public String id;
        public String name;
    }
}
