package de.mur1.dec;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.widget.Button;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private Context context;

    private SurfaceHolder surface_holder;
    private Camera camera;
    private ExecutorService executor_service;
    private Handler main_thread_handler;

    private Semaphore semaphore;

    private int process_frame_skip_counter = 0;

    public byte[] label_map = null;
    public byte[] pipeline_cfg = null;
    public byte[] saved_model = null;

    private void ReadRawFileIntoByteArray(int id) {

        int bytes_read;
        int file_size = 0;
        InputStream is;
        is = context.getResources().openRawResource(id);

        byte[] buffer = new byte[1024];

        try {
            while ((bytes_read = is.read(buffer)) != -1) {
                file_size += bytes_read;
            }
            is.reset();
            bytes_read = -1;

            if (id == R.raw.labels) {
                label_map = new byte[file_size];
            } else if (id == R.raw.pbtxt) {
                pipeline_cfg = new byte[file_size];
            } else if (id == R.raw.graph) {
                saved_model = new byte[file_size];
            }

            file_size = 0;
            while ((bytes_read = is.read(buffer)) != -1) {
                    for (int b = 0; b < bytes_read; b++) {
                        if (id == R.raw.labels) {
                            label_map[file_size + b] = buffer[b];
                        } else if (id == R.raw.pbtxt) {
                            pipeline_cfg[file_size + b] = buffer[b];
                        } else if (id == R.raw.graph) {
                            saved_model[file_size + b] = buffer[b];
                        }
                    }
                    file_size += bytes_read;
            }
        } catch (Exception e) {
            if (id == R.raw.labels) {
                Log.d("error loading file", "label_map");
            } else if (id == R.raw.pbtxt) {
                Log.d("error loading file", "pipeline_cfg");
            } else if (id == R.raw.graph) {
                Log.d("error loading file", "saved_model");
            }
        }
    }

    public CameraPreview(Context context, Camera camera, ExecutorService executor_service, Handler main_thread_handler) {
        super(context);
        this.context = context;
        this.camera = camera;

        this.surface_holder = getHolder();
        this.surface_holder.addCallback(this);

        this.surface_holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        this.executor_service = executor_service;
        this.main_thread_handler = main_thread_handler;
        this.semaphore = new Semaphore(1);

        if (this.label_map == null) {
            ReadRawFileIntoByteArray(R.raw.labels);
        }
        if (this.pipeline_cfg == null) {
            ReadRawFileIntoByteArray(R.raw.pbtxt);
        }
        if (this.saved_model == null) {
            ReadRawFileIntoByteArray(R.raw.graph);
        }

        ProcessFrame.initNet(label_map, label_map.length, pipeline_cfg, pipeline_cfg.length, saved_model, saved_model.length);
        ProcessFrame.init();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            this.camera.setPreviewCallback(this);
            this.camera.setPreviewDisplay(holder);
            this.camera.startPreview();
        } catch (IOException e) {
            //Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        if (this.surface_holder.getSurface() == null){
            return;
        }

        try {
            this.camera.stopPreview();
        } catch (Exception e){
        }

        try {
            this.camera.setDisplayOrientation(90);
            this.camera.setPreviewDisplay(surface_holder);
            this.camera.startPreview();

        } catch (Exception e){
            //Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    int image_counter = 0;

    public void onPreviewFrame(byte[] data, Camera camera) {
        this.process_frame_skip_counter = (this.process_frame_skip_counter + 1) % 10;
        if (this.process_frame_skip_counter != 0) return;

        class ProcessFrameWorker implements Runnable {
            Context context;
            Semaphore semaphore;
            byte[] data;
            Camera camera;
            String avg_coords;

            public ProcessFrameWorker(Semaphore semaphore, byte[] data, Camera camera, Context context) {
                this.context = context;
                this.semaphore = semaphore;
                this.data = data;
                this.camera = camera;
            }

            @Override
            public void run() {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;

                Camera.Parameters parameters = camera.getParameters();
                int width = parameters.getPreviewSize().width;
                int height = parameters.getPreviewSize().height;
                ByteArrayOutputStream outstr = new ByteArrayOutputStream();
                Rect rect = new Rect(0, 0, width, height);
                YuvImage yuvimage=new YuvImage(data, ImageFormat.NV21,width,height,null);
                yuvimage.compressToJpeg(rect, 100, outstr);
                Bitmap bitmap = BitmapFactory.decodeByteArray(outstr.toByteArray(), 0, outstr.size());

                if (bitmap != null) {
                    ProcessFrame.process(bitmap);

                    /*
                    if (bitmap.getPixel(0, 0) == 0) {
                        String path = Environment.getExternalStorageDirectory().toString();
                        OutputStream fOut = null;
                        File file = new File(path + "/Pictures", "edgetest_" + image_counter++ + ".jpg");
                        if (!file.exists()) {
                            try {
                                fOut = new FileOutputStream(file);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                byte[] arr = stream.toByteArray();
                                fOut.write(arr, 0, arr.length);
                                fOut.flush();
                                fOut.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                    */
                }
                this.semaphore.release();
            }
        }

        if (this.semaphore.tryAcquire()) {
            try {
                this.executor_service.execute(new ProcessFrameWorker(this.semaphore, data, camera, context));
            } catch (Exception e) {
                Log.d("process_frame_es", "trying to execute on full queue");
            }
        }
    }
}
