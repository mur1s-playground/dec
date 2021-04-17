package de.mur1.dec;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;

import java.io.InputStream;
import java.lang.reflect.Array;

public class Util {
    public static Context context;
    public static ArrayMap<Integer, byte[]> loaded_resources = new ArrayMap<Integer, byte[]>();

    public static byte[] getResourceAsByteArray(int resource_id) {
        if (!loaded_resources.containsKey(resource_id)) {
            loaded_resources.put(resource_id, ReadRawFileIntoByteArray(resource_id));
        }
        return loaded_resources.get(resource_id);
    }

    private static byte[] ReadRawFileIntoByteArray(int resource_id) {
        int bytes_read;
        int file_size = 0;
        InputStream is;
        is = context.getResources().openRawResource(resource_id);

        byte[] buffer = new byte[1024];

        byte[] result = null;
        try {
            while ((bytes_read = is.read(buffer)) != -1) {
                file_size += bytes_read;
            }
            is.reset();
            result = new byte[file_size];

            file_size = 0;
            while ((bytes_read = is.read(buffer)) != -1) {
                for (int b = 0; b < bytes_read; b++) {
                    result[file_size + b] = buffer[b];
                }
                file_size += bytes_read;
            }
        } catch (Exception e) {
            Log.d("error loading file id: ", resource_id + "");
        }
        return result;
    }
}
