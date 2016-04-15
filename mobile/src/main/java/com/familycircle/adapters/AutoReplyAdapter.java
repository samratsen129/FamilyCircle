package com.familycircle.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.familycircle.R;
import java.util.ArrayList;
import java.util.LinkedHashMap;


/**
 * Created by saran.s on 8/4/2015.
 */
public class AutoReplyAdapter extends BaseAdapter {

    private Activity activity;
    private LinkedHashMap<String,String> map;

    public AutoReplyAdapter(Activity activity, LinkedHashMap<String,String> map) {
        this.activity = activity;
        this.map = map;
    }

    public int getCount() {
        return map.size();
    }

    @Override
    public Object getItem(int position) {
        if (map == null)
            return null;
        return map.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.auto_reply_list_item,
                    null);
        }
        String key = (new ArrayList<String>(map.keySet())).get(position);
        String value = (new ArrayList<String>(map.values())).get(position);

        TextView keyView = (TextView) convertView.findViewById(R.id.item_key);
        keyView.setText(key);

        TextView valueView = (TextView) convertView.findViewById(R.id.item_value);
        valueView .setText(value);

        return convertView;
    }
}