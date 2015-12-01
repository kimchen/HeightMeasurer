package com.temp.heightmeasurer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private SurfaceView sv = null;
    private SurfaceHolder sh = null;
    private Camera camera= null;
    private VSeekBar camHeightBar = null;
    private TextView camHeightTxt = null;
    private TextView resultTxt = null;
    private TextView tipTxt = null;
    private SensorManager sensorManager = null;
    private Sensor gyroSensor = null;
    private ImageView mesureBtn = null;
    private ImageView sight = null;

    private boolean mesureHeight = false;
    private float mesureAngleY= 0f;
    private SensorEvent lastSensorEvent = null;
    private int nowHeight = 0;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        sv = (SurfaceView)root.findViewById(R.id.surfaceView);
        sh = sv.getHolder();
        sh.addCallback(surfaceCallback);

        camHeightBar = (VSeekBar)root.findViewById(R.id.camHeightBar);
        camHeightBar.setMax(300);
        camHeightBar.setProgress(150);

        camHeightBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateHeight(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        camHeightTxt = (TextView)root.findViewById(R.id.camHeightTxt);
        resultTxt = (TextView)root.findViewById(R.id.resulttTxt);
        tipTxt =  (TextView)root.findViewById(R.id.tipTxt);
        tipTxt.setText(R.string.mesure_tip);
        updateHeight(150);

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if(true){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.no_sensor_error).setPositiveButton("OK",null).show();
        }
        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                lastSensorEvent = event;
                //resultTxt.setText("X:"+event.values[0]+"\nY:"+event.values[1]+"\nZ:"+event.values[2]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        },gyroSensor,SensorManager.SENSOR_DELAY_NORMAL);

        sight = (ImageView)root.findViewById(R.id.sight);
        mesureBtn = (ImageView)root.findViewById(R.id.rulerBtn);
        mesureBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                tipTxt.setText(R.string.height_tip);
                if(lastSensorEvent != null)
                    mesureAngleY = lastSensorEvent.values[1];
                sight.setColorFilter(Color.argb(255, 0, 88, 255));
                mesureHeight = true;
                return true;
            }
        });
        mesureBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP){
                    if(lastSensorEvent == null || lastSensorEvent.values == null)
                        return false;
                    float nowAngleY = lastSensorEvent.values[1];
                    if(mesureHeight){
                        double dis = Math.tan(Math.abs(mesureAngleY)/180f*Math.PI) * (double)nowHeight;
                        double x = dis/(Math.tan(Math.abs(nowAngleY)/180f*Math.PI));
                        double height = (double)nowHeight - x;
                        resultTxt.setText(getString(R.string.height)+":"+String.format("%.2f",height)+"CM");
                    }else{
                        double dis = Math.tan(Math.abs(nowAngleY)/180f*Math.PI) * (double)nowHeight;
                        resultTxt.setText(getString(R.string.distance)+":"+String.format("%.2f",dis)+"CM");
                    }
                    sight.setColorFilter(null);
                    mesureHeight = false;
                    tipTxt.setText(R.string.mesure_tip);
                }
                return false;
            }
        });
        return root;
    }

    private void updateHeight(int progress){
        nowHeight = progress;
        camHeightTxt.setText(nowHeight+"\nCM");
    }

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            camera = Camera.open();
            try {
                camera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = camera.getParameters();
            List<List<Integer>> supportSizeList = getSupportedPreviewSizes(camera);
            if(supportSizeList!=null){
                int oriW = width;
                int oriH = height;
                int max = Integer.MAX_VALUE;
                for(List<Integer> size : supportSizeList){
                    int temp = Math.abs(size.get(0)-oriW)*Math.abs(size.get(1)-oriH);
                    if(temp < max){
                        width = size.get(0);
                        height = size.get(1);
                        max = temp;
                    }
                }
            }
            parameters.setPreviewSize(width, height);
            parameters.setPictureFormat(PixelFormat.JPEG);
            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);
            camera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        public List<Integer> GetSupportedPreviewFrameRates(Camera cam) {
            List<Integer> list = new ArrayList<Integer>();
            try {
                String KEY_PREVIEW_FRAME_RATE = "preview-frame-rate-values";
                String str = cam.getParameters().get(KEY_PREVIEW_FRAME_RATE);
                String[] arr = str.split(",");
                int cnt = arr.length;
                for (int i = 0; i < cnt; i++) {
                    list.add(Integer.parseInt(arr[i]));
                }
            } catch (Exception e) {
                list = null;
            }
            return list;
        }

        public List<List<Integer>> getSupportedPreviewSizes(Camera cam){
            List<List<Integer>> list = new ArrayList<List<Integer>>();
            try {
                String KEY_PREVIEW_FRAME_SIZES = "preview-size-values";
                String str = cam.getParameters().get(KEY_PREVIEW_FRAME_SIZES);
                String[] arr = str.split(",");
                int cnt = arr.length;
                for (int i = 0; i < cnt; i++) {
                    List<Integer> temp = new ArrayList<Integer>();
                    String[] size = arr[i].split("x");
                    if(size.length!=2)
                        continue;;
                    temp.add(Integer.parseInt(size[0]));
                    temp.add(Integer.parseInt(size[1]));
                    list.add(temp);
                }
            } catch (Exception e) {
                list = null;
            }
            return list;
        }
    };
}
