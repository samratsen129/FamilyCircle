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
import android.widget.FrameLayout;
import android.widget.TextView;
import com.familycircle.R;
import com.familycircle.manager.PubSubManager;
import com.familycircle.sdk.models.ContactModel;
import com.familycircle.sdk.models.ContactsStaticDataModel;
import org.json.JSONException;
import org.json.JSONObject;

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
        animatorSet.end();
        animatorSet.start();

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
        View view = inflater.inflate(R.layout.fragment_phone_data, container, false);

        String userTagId = getArguments().getString("TAG_ID");
        final ContactModel contactByIdTag = ContactsStaticDataModel.getContactByIdTag(userTagId);

        final TextView heartRateLabel = (TextView) view.findViewById(R.id.heartRateLabel);

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

//        Log.e(this.getTag(), "sdff");


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
