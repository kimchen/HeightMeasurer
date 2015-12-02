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
        View root = inflater.inflate(R.layout.fragment_main, container, false);//UI配置檔
        sv = (SurfaceView)root.findViewById(R.id.surfaceView);//抓出要畫鏡頭回傳畫面的SurfaceView
        sh = sv.getHolder();//取得SurfaceView的管理器(SurfaceHolder)
        sh.addCallback(surfaceCallback);//註冊回叫函數-surfaceCallback 到管理器中

        camHeightBar = (VSeekBar)root.findViewById(R.id.camHeightBar);//取得我們自定義的垂直的SeekBar
        camHeightBar.setMax(300);//設定最大值為300 這樣數值就為0~300 也就是鏡頭高度最低為0cm 最高為300cm
        camHeightBar.setProgress(150);//將預設的鏡頭高度設為150cm


        camHeightBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {//創建SeekBar的回叫函數
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {//SeekBar的數值有變動 也就是調整鏡頭高度數值時 會呼叫此函式
                updateHeight(progress);//更新鏡頭高度
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {//調整SeekBar前會呼叫此函式 這邊我們用不到
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {//調整SeekBar後會呼叫此函式 這邊我們用不到
            }
        });
        camHeightTxt = (TextView)root.findViewById(R.id.camHeightTxt);//畫面右方顯示鏡頭高度數字的UI
        resultTxt = (TextView)root.findViewById(R.id.resulttTxt);//畫面上方顯示計算結果數字的UI
        tipTxt =  (TextView)root.findViewById(R.id.tipTxt);//左下角提示的UI
        tipTxt.setText(R.string.mesure_tip);//設置預設的提示內容
        updateHeight(150);

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);//取得Android的感應器管理員
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);//從感應器管理員中取得方向感應器

        if(gyroSensor == null){//如果手機裝置找不到方向感應器 則彈出警告窗 告知使用者此App的功能無法使用
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());//製作彈出警告窗
            builder.setMessage(R.string.no_sensor_error).setPositiveButton("OK",null).show();//顯示警告窗
        }

        sensorManager.registerListener(new SensorEventListener() {//註冊回叫函數給方向感應器
            @Override
            public void onSensorChanged(SensorEvent event) {//方向感應器的三維數值有變化時 會呼叫此函式
                lastSensorEvent = event;//將方向感應器的數值記錄下來
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {//方向感應器的加速值有變化時 會呼叫此函式 這邊我們用不到

            }
        },gyroSensor,SensorManager.SENSOR_DELAY_NORMAL);

        sight = (ImageView)root.findViewById(R.id.sight);//中央準星的UI
        mesureBtn = (ImageView)root.findViewById(R.id.rulerBtn);//下方按鈕的UI
        mesureBtn.setOnLongClickListener(new View.OnLongClickListener() {//註冊按鈕長按時的回叫函式
            @Override
            public boolean onLongClick(View v) {//按鈕長按時會呼叫此函式
                tipTxt.setText(R.string.height_tip);//變更提示UI 顯示現在為量高度模式
                if(lastSensorEvent != null)//如果感應器有數值 則將Y值記錄下來
                    mesureAngleY = lastSensorEvent.values[1];
                sight.setColorFilter(Color.argb(255, 0, 88, 255));//將中央準星變為藍色 明確提示使用者現在為量高度模式
                mesureHeight = true;//進入量高度模式
                return true;
            }
        });
        mesureBtn.setOnTouchListener(new View.OnTouchListener() {//註冊下方按鈕在按下 及 放開時的回叫函式
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP){//判斷現在為放開按鈕的動作
                    if(lastSensorEvent == null || lastSensorEvent.values == null)//方向感應器沒有數值的話 離開此函式 代表手機沒有方向感應器 此APP功能無法使用
                        return false;
                    float nowAngleY = lastSensorEvent.values[1];//取得方向感應器的Y值
                    if(mesureHeight){//若是量高度模式 則進入此區塊
                        double dis = Math.tan(Math.abs(mesureAngleY)/180f*Math.PI) * (double)nowHeight;//利用三角函數計算手機與目標底端的距離
                        double x = dis/(Math.tan(Math.abs(nowAngleY)/180f*Math.PI));//利用三角函數計算手機與目標的高度差
                        double height = (double)nowHeight - x;//手機高度減去高度差 就為目標的高度 
                        resultTxt.setText(getString(R.string.height)+":"+String.format("%.2f",height)+"CM");//顯示計算結果到UI上
                    }else{//若不是量高度模式 代表是量距離模式 則進入此區塊
                        double dis = Math.tan(Math.abs(nowAngleY)/180f*Math.PI) * (double)nowHeight;//利用三角函數計算手機與目標底端的距離
                        resultTxt.setText(getString(R.string.distance)+":"+String.format("%.2f",dis)+"CM");//顯示計算結果到UI上
                    }
                    sight.setColorFilter(null);//將中央準星設為預設顏色(紅色)
                    mesureHeight = false;//重製量高度模式
                    tipTxt.setText(R.string.mesure_tip);//重製提示UI
                }
                return false;
            }
        });
        return root;
    }

    private void updateHeight(int progress){
        nowHeight = progress;//紀錄當前鏡頭高度
        camHeightTxt.setText(nowHeight+"\nCM");//設置鏡頭高度UI
    }

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {//給SurfaceHolder使用的回叫函式
        @Override
        public void surfaceCreated(SurfaceHolder holder) {//SurfaceView在創建完成時 會呼叫此函式
            camera = Camera.open();//打開手機裝置的鏡頭 並把裝置記錄下來
            try {
                camera.setPreviewDisplay(holder);//將鏡頭預覽畫面傳到 SurfaceHolder 使畫面顯示到SurfaceView上
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {//SurfaceView在變動時 會呼叫此函式
            Camera.Parameters parameters = camera.getParameters();
            List<List<Integer>> supportSizeList = getSupportedPreviewSizes(camera);//利用自定義的函式取得所有裝置支援的解析度
            if(supportSizeList!=null){//一一比對並取得最接近SurfaceView長寬的解析度
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
            parameters.setPreviewSize(width, height);//設置剛剛取得的解析度到鏡頭裝置上
            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);//預設的鏡頭為橫向 而我們的APP是直向 所以需要將畫面旋轉90度
            camera.startPreview();//開始鏡頭畫面預覽
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {//SurfaceView在關閉時 會呼叫此函式
            camera.stopPreview();//停止手機鏡頭的預覽
            camera.release();//釋放鏡頭的資源
            camera = null;
        }

        public List<Integer> GetSupportedPreviewFrameRates(Camera cam) {//自定義的取得手機鏡頭預覽支援更新率的函式 程式中沒用到
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

        public List<List<Integer>> getSupportedPreviewSizes(Camera cam){//自定義的取得手機鏡頭預覽支援解析度的函式
            List<List<Integer>> list = new ArrayList<List<Integer>>();
            try {
                String KEY_PREVIEW_FRAME_SIZES = "preview-size-values";
                String str = cam.getParameters().get(KEY_PREVIEW_FRAME_SIZES);//取得所有的手機鏡頭預覽支援的解析度
                String[] arr = str.split(",");//轉成數字陣列方便後續使用
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
