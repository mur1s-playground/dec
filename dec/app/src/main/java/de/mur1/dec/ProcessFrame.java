package de.mur1.dec;

import android.graphics.Bitmap;

public class ProcessFrame {
    static {
        System.loadLibrary("processframe-lib");
    }

    public static native void init();
    public static native void initNet(byte[] labels, int labels_size, byte[] config, int config_size, byte[] weights, int weights_size);
    public static native void process(Bitmap bitmap);
}
