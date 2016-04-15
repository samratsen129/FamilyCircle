package com.familycircle.lib;

import org.webrtc.VideoRendererGui.ScalingType;

/**
 * Call control interface for container activity.
 */
public interface OnCallEvents {
    public void onCallHangUp();

    public void onCameraSwitch();

    public void onVideoScalingSwitch(ScalingType scalingType);
}
