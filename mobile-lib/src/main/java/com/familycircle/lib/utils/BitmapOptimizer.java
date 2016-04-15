// (c) 2013 AT&T Intellectual Property. All rights reserved.

package com.familycircle.lib.utils;

import android.graphics.BitmapFactory;

public class BitmapOptimizer
{

	public static BitmapFactory.Options getDLBitmapOptions()
	{

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inDither = false;
		bitmapOptions.inPurgeable = true;

		bitmapOptions.inInputShareable = true;

		bitmapOptions.inTempStorage = new byte[16 * 1024];

		return bitmapOptions;
	}

	public static BitmapFactory.Options getDLScaledBitmapOptions(int scaleFactor)
	{

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inSampleSize = scaleFactor;

		return bitmapOptions;
	}

}
