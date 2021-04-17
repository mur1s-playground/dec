package de.mur1.dec;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    class MyCamera {
        public Camera camera = null;

        private Camera.Parameters camera_parameters = null;
        private int camera_id = -1;

        private boolean has_flash = false;
        private boolean flash_on = false;

        private boolean has_focus_mode_continous_picture = false;

        private boolean is_open = true;

        private CameraPreview camera_preview = null;

        private ExecutorService executor_service = null;
        private Handler main_thread_handler = null;

        private Context context;

        public MyCamera(Context context, int id) {
            this.context = context;
            this.camera_id = id;
            this.camera = Camera.open(this.camera_id);;
            this.camera_parameters = camera.getParameters();

            List<String> supportedFocusModes = this.camera_parameters.getSupportedFocusModes();
            if (supportedFocusModes != null) {
                for (int i = 0; i < supportedFocusModes.size(); i++) {
                    if (supportedFocusModes.get(i) == "continuous-picture") {
                        has_focus_mode_continous_picture = true;
                    }
                    Log.v("camera", "focus mode supported: " + supportedFocusModes.get(i));
                }
            }
            List<String> supportedFlashModes = this.camera_parameters.getSupportedFlashModes();
            if (supportedFlashModes != null) {
                for (int i = 0; i < supportedFlashModes.size(); i++) {
                    Log.v("camera", "flash mode supported: " + supportedFocusModes.get(i));
                    has_flash = true;
                }
            }

            this.camera.release();
            is_open = false;
        }

        public void open() {
            if (!is_open) {
                this.camera = Camera.open(this.camera_id);
                this.camera_parameters = camera.getParameters();
                is_open = true;
                if (has_focus_mode_continous_picture) {
                    this.camera_parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                this.camera.setParameters(this.camera_parameters);

                executor_service = Executors.newSingleThreadExecutor();
                main_thread_handler = Handler.createAsync(Looper.getMainLooper());
                camera_preview = new CameraPreview(context, camera, this.executor_service, this.main_thread_handler);
            }
        }

        public void close() {
            if (is_open) {
                this.camera.release();
                is_open = !is_open;
            }
        }

        public boolean isHas_flash() {
            return has_flash;
        }

        public boolean isHas_flash_on() {
            return flash_on;
        }

        public void toggleFlash() {
            if (has_flash) {
                if (flash_on) {
                    this.camera_parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    flash_on = !flash_on;
                } else {
                    this.camera_parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    flash_on = !flash_on;
                }
                this.camera.setParameters(this.camera_parameters);
            }
        }

        public CameraPreview getCamera_preview() {
            return camera_preview;
        }
    }

    private ArrayList<MyCamera> my_cameras = new ArrayList<MyCamera>();

    private int camera_c = 0;
    private int camera_id = -1;

    Camera.ErrorCallback camera_error = null;

    public void nextCamera(View v) {
        camera_id = (camera_id + 1) % camera_c;
        my_cameras.get(camera_id).open();

        Button flash_button = (Button) findViewById(R.id.flashlight_toggle_button);
        if (my_cameras.get(camera_id).isHas_flash()) {
            flash_button.setAlpha(1.0f);
            flash_button.setEnabled(true);
        } else {
            flash_button.setAlpha(0.0f);
            flash_button.setEnabled(false);
        }

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.removeAllViews();
        preview.addView(my_cameras.get(camera_id).getCamera_preview());
    }

    public void toggleCameraFlashlight(View v) {
         Button flash_button = (Button) findViewById(R.id.flashlight_toggle_button);
         if (my_cameras.get(camera_id).isHas_flash_on()) {
             Drawable img = this.getResources().getDrawable(R.drawable.ic_action_flash_on);
             flash_button.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
         } else {
             Drawable img = this.getResources().getDrawable(R.drawable.ic_action_flash_off);
             flash_button.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
         }
         my_cameras.get(camera_id).toggleFlash();
    }

    private boolean hasCamera(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            this.camera_c = Camera.getNumberOfCameras();
            for (int i = 0; i < this.camera_c; i++) {
                /*
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    Log.v("camera", "back facing id:" + i);
                }
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    Log.v("camera", "front facing id: " + i);
                }
                */
                MyCamera my_cam = new MyCamera(context, i);
                my_cameras.add(my_cam);
            }
            Button switch_camera_button = (Button) findViewById(R.id.switch_camera_button);
            if (camera_c > 1) {
                switch_camera_button.setAlpha(1.0f);
                switch_camera_button.setEnabled(true);
            } else {
                switch_camera_button.setAlpha(0.0f);
                switch_camera_button.setEnabled(false);
            }
            return true;
        } else {
            return false;
        }
    }

    class CameraErrorCallback implements Camera.ErrorCallback {
        public void onError(int error, Camera camera) {
            Log.e("camera", "error id: " + error);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        try {
            this.getSupportActionBar().hide();
        } catch(Exception e) {

        }
        camera_error = new CameraErrorCallback();

        setCamera();
    }

    private void setCamera() {
        if (this.hasCamera(this)) {
            View v = (View) findViewById(R.id.content);
            nextCamera(v);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera_id >= 0 && camera_id < my_cameras.size()) {
            my_cameras.get(camera_id).close();
        }
    }
}