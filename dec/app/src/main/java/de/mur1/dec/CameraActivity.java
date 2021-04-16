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
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private int camera_c = 0;
    private int camera_id = 0;
    private Camera camera = null;
    private Camera.Parameters camera_parameters = null;
    private boolean camera_flashlight = false;

    private CameraPreview camera_preview = null;

    private ExecutorService executor_service = null;
    private Handler main_thread_handler = null;

    public void toggleCameraFlashlight(View v) {
        if (this.camera_parameters != null) {
            Button flash_button = (Button) findViewById(R.id.flashlight_toggle_button);
            if (!this.camera_flashlight) {
                this.camera_parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                this.camera.setParameters(this.camera_parameters);
                Drawable img = this.getResources().getDrawable(R.drawable.ic_action_flash_off);
                flash_button.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
                this.camera_flashlight = true;
            } else {
                this.camera_parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                this.camera.setParameters(this.camera_parameters);
                Drawable img = this.getResources().getDrawable(R.drawable.ic_action_flash_on);
                flash_button.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
                this.camera_flashlight = false;
            }
        }
    }

    private boolean hasCamera(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            this.camera_c = Camera.getNumberOfCameras();
            return true;
        } else {
            return false;
        }
    }

    public boolean openCameraInstance(){
        try {
            for (int i = 0; i < camera_c; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                this.camera_id = i;
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    break;
                }
                /*
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    break;
                }
                */
            }
            this.camera = Camera.open(this.camera_id);
            this.camera_parameters = this.camera.getParameters();
            this.camera_parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            this.camera.setParameters(this.camera_parameters);
            return true;
        } catch (Exception e){
            this.camera = null;
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        executor_service = Executors.newSingleThreadExecutor();
        main_thread_handler = Handler.createAsync(Looper.getMainLooper());

        setCamera();
    }

    private void setCamera() {
        if (this.hasCamera(this)) {
            if (this.openCameraInstance()) {
                camera_preview = new CameraPreview(this, this.camera, this.executor_service, this.main_thread_handler);
                FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
                preview.addView(camera_preview);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.camera != null) {
            this.camera.release();
            this.camera = null;
            this.camera_parameters = null;
        }
    }
}