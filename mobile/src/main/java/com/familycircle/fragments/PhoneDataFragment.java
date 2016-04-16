package com.familycircle.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.familycircle.R;
import com.familycircle.manager.PubSubManager;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import com.familycircle.utils.network.M2XGetStreamValues;
import com.familycircle.utils.network.QueryDbUser;
import com.familycircle.utils.network.Response;
import com.familycircle.utils.network.ResponseListener;
import com.familycircle.utils.network.model.M2XValuesModel;
import com.familycircle.utils.network.model.UserObject;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by michaelm on 4/16/2016.
 */
public class PhoneDataFragment extends Fragment {
    private PubSubManager instance = null;
    private PubSubManager.OnPubNubMessage pubNubListener = null;
    private HeartRateView heartRateView;
    private AnimatorSet animatorSet;

    public PhoneDataFragment() {
    }

    private void setHeartRate(final float rate) {
//                animatorSet.cancel();
//        animatorSet.end();
//        animatorSet.start();

        this.rate = rate;
    }

    private float rate = 0;

    private void setUpHeartRateAnimations() {
        final int animationDuration = 200;
        ObjectAnimator animStart = ObjectAnimator.ofInt(heartRateView, "radius", heartRateView.maxRadius / 2, heartRateView.maxRadius);
        ObjectAnimator animEnd = ObjectAnimator.ofInt(heartRateView, "radius", heartRateView.maxRadius, heartRateView.maxRadius / 2);
        animStart.setDuration(animationDuration / 2);
        animEnd.setDuration(animationDuration / 2);
        animatorSet = new AnimatorSet();
//        final AnimatorSet animations = animatorSet;
        animatorSet.playSequentially(animStart, animEnd);
        animatorSet.setDuration(animationDuration);

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (rate == 0) {
                    return;
                }
                final float perMs = 1 / (rate / 60) * 1000;
                animatorSet.setStartDelay((long) (perMs - animationDuration));
                animatorSet.start();
            }
        });

        animatorSet.start();

//        anim

    }

    private static class HeartRateView extends View {
        public int radius = 100;
        public int maxRadius = 0;
        private Paint paint = new Paint() {
            {
                setColor(getResources().getColor(R.color.black));
                setStyle(Style.FILL);
                setAntiAlias(true);
            }
        };

        public HeartRateView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            maxRadius = getBottom();
            canvas.drawCircle(getRight() / 2, getBottom() / 2, radius / 2, paint);
        }


        public int getRadius() {
            return radius;
        }

        public void setRadius(int radius) {
            this.radius = radius;
            invalidate();
        }

    }

    private HeartRateView createHeartRateView() {
        return new HeartRateView(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_phone_data, container, false);

        String userTagId = getArguments().getString("TAG_ID");
        final ContactModel contactByIdTag = ContactsStaticDataModel.getContactByIdTag(userTagId);

        final TextView heartRateLabel = (TextView) view.findViewById(R.id.heartRateLabel);
        final TextView phoneDataHeading = (TextView) view.findViewById(R.id.phoneDataHeading);

        ListView speedDataList = (ListView) view.findViewById(R.id.speedDataListView);

        phoneDataHeading.setText("Data for " + (contactByIdTag != null ? contactByIdTag.getIdTag() : ""));

        FrameLayout heartContainer = (FrameLayout) view.findViewById(R.id.heartContainerView);
        heartContainer.removeAllViews();
        heartRateView = createHeartRateView();
        heartContainer.addView(heartRateView);

        instance = PubSubManager.getInstance();
        pubNubListener = new PubSubManager.OnPubNubMessage() {
            boolean animationsSetUp = false;

            @Override
            public void onPubNubMessage(String channel, Object message, final JSONObject jsonObject) {
                Log.e(getTag(), jsonObject + "");
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (contactByIdTag != null && contactByIdTag.getEmail() != null && jsonObject != null && contactByIdTag.getEmail().equals(jsonObject.get("from"))) {
                                String type = jsonObject.getString("type");
                                String value = jsonObject.getString("value");

                                switch (type) {
                                    case "heartbeat":

                                        if (!animationsSetUp) {
                                            setUpHeartRateAnimations();
                                        }
                                        animationsSetUp = true;

                                        heartRateLabel.setText(String.format("%s BPM", value));
                                        setHeartRate(Float.parseFloat(value));

                                        break;
                                    default:
                                        break;

                                }
                                Log.e(getTag(), type);
                                Log.e(getTag(), value);
                            }
                        } catch (JSONException e) {
                            //if a field is missing or anything... can't really do anything about it anyway
                            Log.e(getTag(), e.getMessage());
                        }
                    }
                }, 0);
            }

            @Override
            public void onConnect(String channel, Object message) {
            }
        };
        instance.addListener(pubNubListener);


        final ArrayAdapter<String> speedAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_item_speed);
        speedDataList.setAdapter(speedAdapter);
        final LineChart chart = (LineChart) view.findViewById(R.id.chart);


        if(contactByIdTag != null) {
            new QueryDbUser(contactByIdTag.getIdTag(), new ResponseListener() {
                @Override
                public void onSuccess(Response response) {
                    UserObject model = (UserObject) response.getModel();

                    new M2XGetStreamValues(new ResponseListener() {
                        @Override
                        public void onSuccess(Response response) {
                            Log.e(getTag(), response.getModel()+"");
                            M2XValuesModel values = (M2XValuesModel) response.getModel();

                            for (M2XValuesModel.ValueModel speedValue : values.m2xValues) {
                                speedAdapter.add(speedValue.value);
                            }
                        }

                        @Override
                        public void onFailure(Response response) {

                        }
                    }, model.m2x_id, "distance", 100).exec();
                    new M2XGetStreamValues(new ResponseListener() {
                        @Override
                        public void onSuccess(Response response) {
                            Log.e(getTag(), response.getModel()+"");
                            M2XValuesModel values = (M2XValuesModel) response.getModel();

                            float min = 1000;
                            float max = 0;
                            int idx = 0;
                            ArrayList<Entry> heartbeatVals = new ArrayList<>();
                            String[] heartbeatAxis = new String[values.m2xValues.size()];
                            for (M2XValuesModel.ValueModel heartbeatValue : values.m2xValues) {
                                float value = Float.parseFloat(heartbeatValue.value);
                                heartbeatVals.add(new Entry(value, idx));
                                heartbeatAxis[idx] = idx+"";
                                if(value > max) {
                                    max = value;
                                }
                                if(value < min) {
                                    min = value;
                                }
                                ++idx;
                            }
                            LineDataSet dataSet = new LineDataSet(heartbeatVals, "Heartbeat");
                            chart.setData(new LineData(heartbeatAxis, dataSet));
                            chart.notifyDataSetChanged();
                            chart.invalidate();
                        }

                        @Override
                        public void onFailure(Response response) {

                        }
                    }, model.m2x_id, "heartbeat", 100).exec();
                }

                @Override
                public void onFailure(Response response) {

                }
            }).exec();
        }


        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (instance != null && pubNubListener != null) {
            instance.removeListener(pubNubListener);
        } else if (pubNubListener == null) {
            Log.e(getTag(), "Failed to close pubnub listener");
        }
    }
}
