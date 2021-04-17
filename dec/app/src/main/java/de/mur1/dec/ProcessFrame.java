package de.mur1.dec;

import android.graphics.Bitmap;

public class ProcessFrame {
    public static boolean initialized = false;

    static {
        System.loadLibrary("processframe-lib");
    }

    public static native void setSettings(int[] classFilter, int class_filter_size, float confidence_threshold, float horizontal_edge_threshold, float vertical_edge_threshold, float horizontal_size_thres, float vertical_size_thres);
    public static native void init();
    public static native void initNet(byte[] labels, int labels_size, byte[] config, int config_size, byte[] weights, int weights_size);
    public static native void process(Bitmap bitmap);
}
