package com.familycircle.utils.network.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samratsen on 4/15/16.
 */
public class M2XValuesModel extends BaseModel{

    public List<ValueModel> m2xValues = new ArrayList<ValueModel>();

    public static class ValueModel {
        public String timestamp;
        public String value;
    }
}
