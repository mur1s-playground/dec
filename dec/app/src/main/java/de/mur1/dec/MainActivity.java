package de.mur1.dec;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST_CODE = 100;

    String[] classes = null;

    private static int[] selected_classes = null;

    class MyMultiAutoCompleteTextView extends MultiAutoCompleteTextView {

        public MyMultiAutoCompleteTextView(Context context) {
            super(context);
        }

        @Override
        public boolean enoughToFilter() {
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.context = this;
        setContentView(R.layout.activity_main);

        byte[] labels = Util.getResourceAsByteArray(R.raw.labels);
        String labels_str = new String(labels);
        classes = labels_str.split("\n");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, classes);
        MultiAutoCompleteTextView textViewClasses = (MultiAutoCompleteTextView) findViewById(R.id.multiAutoCompleteTextView_classes);
        textViewClasses.setAdapter(adapter);
        textViewClasses.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        textViewClasses.setThreshold(0);
    }

    public void openCamera(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
        } else {
            MultiAutoCompleteTextView textViewClasses = (MultiAutoCompleteTextView) findViewById(R.id.multiAutoCompleteTextView_classes);
            String selected = textViewClasses.getText().toString();
            String[] selected_arr = selected.split(",");

            ArrayList<Integer> tmp_selected = new ArrayList<Integer>();
            for (int i = 0; i < selected_arr.length; i++) {
                boolean found = false;
                for (int j = 0; j < classes.length; j++) {
                    if (selected_arr[i].trim().length() > 0 && selected_arr[i].trim().equals(classes[j])) {
                        tmp_selected.add(j);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Log.e("class selection", "class not found: " + selected_arr[i]);
                }
            }
            if (tmp_selected.size() > 0) {
                selected_classes = new int[tmp_selected.size()];
                for (int i = 0; i < tmp_selected.size(); i++) {
                    selected_classes[i] = tmp_selected.get(i);
                }
            }

            EditText confidence_thres = (EditText) findViewById(R.id.editTextNumber_conf);
            float confidence = (float)Integer.valueOf(confidence_thres.getText().toString())/100.0f;

            EditText horizontal_edge_thres = (EditText) findViewById(R.id.editTextNumber);
            float horizontal_thres = (float)Integer.valueOf(horizontal_edge_thres.getText().toString())/100.0f;

            EditText vertical_edge_thres = (EditText) findViewById(R.id.editTextNumber_v);
            float vertical_thres = (float)Integer.valueOf(vertical_edge_thres.getText().toString())/100.0f;

            EditText horizontal_size_thres = (EditText) findViewById(R.id.editTextNumber_s);
            float horizontal_s_thres = (float)Integer.valueOf(horizontal_edge_thres.getText().toString())/100.0f;

            EditText vertical_size_thres = (EditText) findViewById(R.id.editTextNumber_vs);
            float vertical_s_thres = (float)Integer.valueOf(vertical_size_thres.getText().toString())/100.0f;


            ProcessFrame.setSettings(selected_classes, tmp_selected.size(), confidence, horizontal_thres, vertical_thres, horizontal_s_thres, vertical_s_thres);

            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        }
    }
}