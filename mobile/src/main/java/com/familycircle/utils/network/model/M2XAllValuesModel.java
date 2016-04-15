package com.familycircle.utils.network.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by samratsen on 4/15/16.
 */
public class M2XAllValuesModel extends BaseModel{

    public List<ValuesModel> m2xValues = new ArrayList<ValuesModel>();

    public static class ValuesModel {
        public String timestamp;
        public Map<String, String> valueMap = new HashMap<String, String>();
    }
}
