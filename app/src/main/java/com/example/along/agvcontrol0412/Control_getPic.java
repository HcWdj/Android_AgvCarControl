package com.example.along.agvcontrol0412;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import MyClass.DockSite;
import MyClass.LimitLine;
import MyClass.MyPicture;
import MyClass.NavigationMessage;
import MyInterface.UpMap;
import MyInterface.UpMapMes;
import okhttp3.ResponseBody;
import recev.Map_xy;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import cz.msebera.android.httpclient.Header;
import send.SendSetSpeed;
import socket.RockerView;
import socket.TcpClient;

/**
 *
 * 改进点： limitLines的集合从List改为不重复的set
 * dockSites.clear();还没加上！！！一定要找地方加
 *
 *
 */

public class Control_getPic extends AppCompatActivity implements View.OnClickListener{

    private String baseUrl="http://192.168.1.101/";
    public static Bitmap bitmap;
    private byte[] bitmapbuff;//用来显示编辑的图
    private Toolbar toolbar;
    private RockerView mRockerView;
    private boolean widgetState=false;
    private static ImageView c_imageView;
    private TextView edsetLineSpeed,edsetAngleSpeed,txLineSpeed,txAngleSpeed,dispLineSp,dispAngleSp;
    public static TextView tv_voltage;
    private Button bnConfirmSpeed;
    private static boolean sendSpeedOrNot=true;
    private static int Lx=0,Ly=0;
    public static float LineSpeed=(float) 0.5,AngleSpeed=(float)0.5;
    public static final int CHOOSE_PHOTO=2;
    private boolean drawOrNot=false;//判断是否可以进行标记、编辑功能 防止误触屏幕操作
    private static List<LimitLine> limitLines=new ArrayList<>();     //限行区域
    private static List<LimitLine> limitLineslast=new ArrayList<>();
    private int limitLinePointR=2;
    public static HashMap<String, DockSite> dockSites=new HashMap<>();//停靠点
    private static String fileName;
    //-----------------stereo------------------
    private static boolean stereoFlag=false;

    //-----------------------------------------

    private TcpClient client=TcpClient.getInstance();
    private SendSetSpeed sendSetSpeed=SendSetSpeed.getInstance_SendSetSpeed();
    private MyPicture myPicture=MyPicture.getInstance_myPicture();
    private Map_xy map_xy=Map_xy.getInstance_map_xy();
    private NavigationMessage navigationMessage=NavigationMessage.getInstance_navigationMessage();

    private Handler handler =new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_get_pic);

        ToolBarInit();//工具栏初始化

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},123);
        }
        //***对接xml***//
        mRockerView = findViewById(R.id.my_rocker);
        edsetLineSpeed=findViewById(R.id.ed_setLineSpeed);
        edsetAngleSpeed=findViewById(R.id.ed_setAngleSpeed);
        txLineSpeed=findViewById(R.id.tx_lineSpeed);
        txAngleSpeed=findViewById(R.id.tx_angleSpeed);
        dispLineSp=findViewById(R.id.tx_maxLineSpeed);
        dispAngleSp=findViewById(R.id.tx_maxAngleSpeed);
        bnConfirmSpeed = findViewById(R.id.bn_ConfirmSpeed);
        tv_voltage=findViewById(R.id.ctl_batteryVoltage);
//        Button btcreatePic=findViewById(R.id.bt_createPic);
//        Button btsaveMap=findViewById(R.id.bt_saveMap);
//        Button btcharge=findViewById(R.id.bt_charge);
        c_imageView=this.findViewById(R.id.c_image_View);
        c_imageView.setOnTouchListener(new TouchListener());
        //***监听***//
        bnConfirmSpeed.setOnClickListener(this);
//        btsaveMap.setOnClickListener(this);
//        btcreatePic.setOnClickListener(this);
//        btcharge.setOnClickListener(this);
        //bnsavePic.setOnClickListener(this);
        //bnloadPic.setOnClickListener(this);
        //***摇杆返回坐标***//
        mRockerView.setOnLocation(new RockerView.OnLocationListener() {
            @Override
            public void onLocation(float x, float y) {
                Lx=(int)x;  //角速度
                Ly=(int)-y; //线速度
//                Log.i("Lx",Lx+"");
//                Log.i("Ly",Ly+"");
                float Sx,Sy;
                if(Lx==0)
                    Sx=0;
                else {
                    Sx = (float) (Math.round((float) Lx*AngleSpeed/(127) * 1000)) / 1000;
                }
                if(Ly==0)
                    Sy=0;
                else
                    Sy = (float) (Math.round((float) Ly*LineSpeed / (127) * 1000)) / 1000;

                txLineSpeed.setText(String.format("%s",Sy)); //显示角速度
                txAngleSpeed.setText(String.format("%s",Sx));  //显示线速度
            }
        });
    }
    @Override
    protected void onStop(){
        super.onStop();
        sendSpeedOrNot=false;
        client.clearStereoPoints();
    }
    @Override
    protected void onResume() {
        super.onResume();
        toolbar.setTitle("建图模式");
        if (MainActivity.isReceive_flag()) {
            sendSpeedOrNot = true;
            client.sendSpeed();
        }
    }

    private void ToolBarInit(){
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.mytoolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int menuItemId=menuItem.getItemId();
                switch (menuItemId){
                    case R.id.id_item_transfer://转换模式
//                        writeInPhone("test1","hello world","hi my world");
                        break;
                    case R.id.id_item_mapping://开始建图
                        //-----stereo-------
                        stereoFlag=false;
                        //------------------
                        drawOrNot=false;
                        client.AskForPic();
                        limitLines.clear();
                        limitLineslast.clear();
                        navigationMessage.setMode(0);//这个set之后，进入导航界面就不显示图片了，否则进入导航界面都会重新显示导航界面，不用每次都推入图片
                        break;
                    case R.id.id_item_edit://开始编辑 ******添加确认框******
                        drawOrNot=true;    //允许规划限行区域，否则会误触屏幕画上去
                        if(MainActivity.isReceive_flag())//如果是持续发图状态
                            client.endPic();   //停止发图，开始编辑
                        bitmapbuff=client.getEditBuff();//接受在建图模式的时候的图(bitmapbuff 一维数组模式)
                        for(int i=0;i<10000;i++)
                            ;
                        drawMap();
                        Log.i("TAG__edit",""+MainActivity.isReceive_flag());
                        break;
                    case R.id.id_item_locate://添加站点
                        recordStation();
                        break;
                    case R.id.id_item_save://保存图片
                        Log.i("TAG__save",""+MainActivity.isReceive_flag());
                        if(MainActivity.isReceive_flag()) {//如果是持续发图状态
                            client.endPic();   //停止发图，开始编辑
                            bitmapbuff = client.getEditBuff();//接受在建图模式的时候的图(bitmapbuff 一维数组模式)
                            for(int i=0;i<10000;i++)
                                ;
                            drawMap();
                        }
                        client.SaveMap();
//                        if(client.getSave_map_success())
//                            SaveMapSuccess();
                        savePicture();   //保存地图在变量里
                        break;
                    case R.id.id_item_upload://上传图像到服务器
                        inputPictureName();//输入图片名，保存在手机里

//                        dockSites.clear();
//                        //命令小车端保存图像
//                        client.SaveMap();
//                        //接收小车发来的消息，判断是否接收成功
//                        client.saveMapOrNot();
//                        //接收成功，显示屏幕提示成功
//                        if(client.getSave_map_success()) {
//                            SaveMapSuccess();
//                        }
                        break;
                    case R.id.id_item_rocker://显示、隐藏控件
                        widgetShow(widgetState);
                        break;
                    case  R.id.id_item_recordCharge:
                        client.recordCharge();
                        break;
                    case R.id.id_item_doubleEye:
                        client.sendStereo();
                        //-----stereo-------
                        stereoFlag=true;
                        //------------------
                        break;
                    case R.id.id_item_doubleEyeIMU:
                        client.sendStereoIMU();
                        break;
                    case R.id.id_item_saveStereo:
                        client.sendSaveStereoMap();
                        client.clearStereoPoints();
                        break;
                }
                return true;
            }
        });
    }

    private void recordStation(){
        final EditText inputServer=new EditText(this);
        AlertDialog.Builder radioDialog=new AlertDialog.Builder(this);
        radioDialog.setTitle("设置站点")
                .setView(inputServer);
        radioDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        radioDialog.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //记录站点
                recordStation(inputServer.getText().toString());
            }
        });
        radioDialog.create().show();
    }
    private void recordStation(String name){
        dockSites.put(name,new DockSite(name,map_xy.getX()*100/5, map_xy.getY()*100/5, map_xy.getAngle()));
    }

    /**
     * 这个是用于在已经有图或者重新编辑情况下
     * @param
     */
    private void drawMap(){
        if(client.getMybitmap()!=null){
            limitLines.clear();  //二次作图的时候因为已经保存过了，所以该集合可以清空
            Bitmap bmp = client.getMybitmap().copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(bmp);
            Paint paint=new Paint();
            paint.setColor(Color.GREEN);
            for(DockSite dockSite:dockSites.values()){
                float x=dockSite.x;
                float y=client.getPicH()-dockSite.y;

                float angle=dockSite.angle;
                canvas.rotate(-angle,x,y);
                Path path=new Path();
                path.moveTo(x+5,y);
                path.lineTo(x-5,y+3);
                path.lineTo(x-5,y-3);
                path.close();
                canvas.drawPath(path,paint);
                canvas.rotate(angle,x,y); //画完后要把画布再翻转回来，防止角度重叠
            }

            paint.setColor(Color.GRAY);
            for(LimitLine point : limitLineslast)
                canvas.drawCircle(point.x, point.y, limitLinePointR, paint);
            c_imageView.setImageBitmap(bmp);
        }
    }
    /**
     * 这个是在编辑图的时候要用到的函数
     * @param limitLines
     * @param myDockSites
     */
    private void drawMap(List<LimitLine> limitLines,HashMap<String,DockSite>myDockSites){
        if(client.getMybitmap()!=null) {//防止误触引发空指针错误
            Bitmap bmp = client.getMybitmap().copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(bmp);
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            Log.i("RED","RED");
            for (LimitLine point : limitLines)
                canvas.drawCircle(point.x, point.y, limitLinePointR, paint);

            paint.setColor(Color.GRAY);
            for(LimitLine point : limitLineslast)
                canvas.drawCircle(point.x, point.y, limitLinePointR, paint);

            paint.setColor(Color.GREEN);
            for(DockSite dockSite:myDockSites.values()){
                float x=dockSite.x;
                float y=client.getPicH()-dockSite.y;
                float angle=dockSite.angle;
                canvas.rotate(-angle,x,y);
                Path path=new Path();
                path.moveTo(x+5,y);
                path.lineTo(x-5,y+3);
                path.lineTo(x-5,y-3);
                path.close();
                canvas.drawPath(path,paint);
                canvas.rotate(angle,x,y); //画完后要把画布再翻转回来，防止角度重叠
            }
            c_imageView.setImageBitmap(bmp);
        }
    }

    private void widgetShow(boolean flag){
//        if(flag==false){
//            widgetState=true;
//            this.findViewById(R.id.control_rocker).setVisibility(View.VISIBLE);
//        }else {
//            widgetState=false;
//            this.findViewById(R.id.control_rocker).setVisibility(View.GONE);
//        }
        this.findViewById(R.id.control_rocker).setVisibility(View.VISIBLE);
    }

    /**
     * 显示菜单
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.mytoolbar,menu);
        return true;
    }

    public void SaveMapSuccess(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Control_getPic.this, "已保存地图", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.bn_ConfirmSpeed:
                setSpeed();
                break;
        }
    }


    private void setSpeed(){
        sendSetSpeed.setLineSpeed(Float.valueOf(edsetLineSpeed.getText().toString()));
        sendSetSpeed.setAngleSpeed(Float.valueOf(edsetAngleSpeed.getText().toString()));
        LineSpeed=sendSetSpeed.getLineSpeed();
        AngleSpeed=sendSetSpeed.getAngleSpeed();

        if(LineSpeed>=2)
            LineSpeed=2;
        if(AngleSpeed>=2)
            AngleSpeed=2;
        client.sendSetSpeed();
        Log.i("control_LineSpeed",sendSetSpeed.getLineSpeed()+"");
        Log.i("control_AngleSpeed",sendSetSpeed.getAngleSpeed()+"");
    }

    /**
     * 编辑地图，用来限行区域
     */
    private final class TouchListener implements View.OnTouchListener{
        private Matrix currentMatrix=new Matrix();
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float point_x,point_y;
            currentMatrix.set(c_imageView.getImageMatrix());
            float sizeW_times = (float) (Math.round((float) c_imageView.getWidth() / client.getPicW() * 1000)) / 1000;//map_xy.getX()
            float sizeH_times = (float) (Math.round((float) c_imageView.getHeight() / client.getPicH() * 1000)) / 1000;//map_xy.getY()
            float size = sizeW_times < sizeH_times ? sizeW_times : sizeH_times;

            float qW = (c_imageView.getWidth() / size - client.getPicW()) / 2;
            point_x = event.getX() / size - qW;
            float qH = (c_imageView.getHeight() / size - client.getPicH()) / 2;
            point_y = event.getY() / size - qH;
            if (point_x > client.getPicW())
                point_x = client.getPicW();
            else if (point_x <= 0)
                point_x = 0;
            if (point_y > client.getPicH())
                point_y = client.getPicH();
            else if (point_y <= 0)
                point_y = 0;
            if(drawOrNot) {//只有在允许画的时候(点击编辑地图的时候才开启)
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        limitLines.add(new LimitLine((int) point_x, (int) point_y));
                        drawMap(limitLines,dockSites);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        limitLines.add(new LimitLine((int) point_x, (int) point_y));
                        drawMap(limitLines,dockSites);
                        break;
                }
            }
            return true;
        }
    }

    /**
     * 保存地图在手机内部变量里
     */
    private void savePicture(){
        //把周围的点全加入限行区域(原来只加了点下去的那个点)，最后画的是limitLineslast
        for(LimitLine limitLine :limitLines) {
            for (int x = limitLine.x - limitLinePointR; x < limitLine.x + limitLinePointR; x++)
                for (int y = limitLine.y - limitLinePointR; y < limitLine.y + limitLinePointR; y++) {
                    limitLineslast.add(new LimitLine(x, y));
                }
        }
        //再扫描一次，把所有点在bitmapbuff中改变颜色
        for(LimitLine limitLine:limitLineslast){
            int location=limitLine.y*client.getPicW()+limitLine.x;
            bitmapbuff[location]=100;
        }
        bitmap=createBitmap(bitmapbuff,client.getPicW(),client.getPicH());
        handler.post(new Runnable() {//保存完地图，只需把停靠点画上就好
            @Override
            public void run() {
                Bitmap bmp=bitmap.copy(Bitmap.Config.ARGB_8888,true);
                Canvas canvas = new Canvas(bmp);
                Paint paint=new Paint();
                paint.setColor(Color.GREEN);
                for(DockSite dockSite:dockSites.values()){
                    float x=dockSite.x;
                    float y=client.getPicH()-dockSite.y;
                    float angle=dockSite.angle;
                    canvas.rotate(-angle,x,y);
                    Path path=new Path();
                    path.moveTo(x+5,y);
                    path.lineTo(x-5,y+3);
                    path.lineTo(x-5,y-3);
                    path.close();
                    canvas.drawPath(path,paint);
                    canvas.rotate(angle,x,y); //画完后要把画布再翻转回来，防止角度重叠
                }
                c_imageView.setImageBitmap(bmp);
            }
        });
        myPicture.setRow(client.getPicH());
        myPicture.setColumn(client.getPicW());
        myPicture.setStation_number(dockSites.size());
        myPicture.setOrigin_x(client.getOriginal_pointX());
        myPicture.setOrigin_y(client.getOriginal_pointY());

        limitLines.clear();
    }

    /**
     * 数组变灰度图
     * @param values 数组
     * @param picW 宽
     * @param picH 长
     * @return 灰度图
     */
    private static Bitmap createBitmap(byte[] values, int picW, int picH) {
        if(values == null || picW <= 0 || picH <= 0)
            return null;
        //使用8位来保存图片
        Bitmap bitmap = Bitmap.createBitmap(picW, picH, Bitmap.Config.ARGB_8888);
        int pixels[] = new int[picW * picH];
        for (int i = 0; i < pixels.length; ++i) {
            //关键代码，生产灰度图
            int temp;
            temp=values[i]>0?values[i]:(values[i]+255);
            pixels[i] = temp * 256 * 256 + temp * 256 + temp + 0xFF000000;//
        }
        bitmap.setPixels(pixels, 0, picW, 0, 0, picW, picH);
        values = null;
        pixels = null;
        return bitmap;
    }


    public static byte[] getByteStream(String fileName){
        try{
            String file=Environment.getExternalStorageDirectory()+"/AgvCar"+"/pbstream"+ "/"+fileName;
            // 拿到输入流
            FileInputStream input = new FileInputStream(file);
            // 建立存储器
            byte[] buf =new byte[input.available()];
            // 读取到存储器
            input.read(buf);
            // 关闭输入流
            input.close();
            // 返回数据
            return buf;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 根据byte数组生成文件
     *
     * @param bytes
     *            生成文件用到的byte数组
     */
    public static void createFileWithByte(byte[] bytes) {
        /**
         * 创建File对象，其中包含文件所在的目录以及文件的命名
         */
        File file = new File(Environment.getExternalStorageDirectory()+"/AgvCar"+"/pbstream",fileName);//
        // 创建FileOutputStream对象
        FileOutputStream outputStream = null;
        // 创建BufferedOutputStream对象
        BufferedOutputStream bufferedOutputStream = null;
        try {
            // 如果文件存在则删除
            if (file.exists()) {
                file.delete();
            }
            // 在文件系统中根据路径创建一个新的空文件
            file.createNewFile();
            // 获取FileOutputStream对象
            outputStream = new FileOutputStream(file);
            // 获取BufferedOutputStream对象
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            // 往文件所在的缓冲输出流中写byte数据
            bufferedOutputStream.write(bytes);
            // 刷出缓冲输出流，该步很关键，要是不执行flush()方法，那么文件的内容是空的。
            bufferedOutputStream.flush();
        } catch (Exception e) {
            // 打印异常信息
            e.printStackTrace();
        } finally {
            // 关闭创建的流对象
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    /**
     * 输入要存储的图像的名字
     */
    private void inputPictureName(){
        final EditText inputServer=new EditText(this);
        AlertDialog.Builder radioDialog=new AlertDialog.Builder(this);
        radioDialog.setTitle("输入名称")
                .setView(inputServer);
        radioDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        radioDialog.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //保存图像到手机里
                fileName=inputServer.getText().toString();
                SavaImageInPhone(bitmap,inputServer.getText().toString());
                //请求pbstream存储在手机里
//                client.AskForPbstream();
                client.receivePbstream();
            }
        });
        radioDialog.create().show();
    }
    /**
     * 保存图像到移动端,如果重名不做任何操作，如果没重名，上传给服务器
     * @param bitmap
     * @param name
     */
    public void SavaImageInPhone(Bitmap bitmap, String name) {
        String picpath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/";
        File file = new File(picpath);
        if (!file.exists())
            file.mkdirs();
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        this.sendBroadcast(intent);
        try {
            File picFile = new File(picpath, name + ".png");
            if(picFile.exists())
                Toast.makeText(Control_getPic.this,"文件重名！",Toast.LENGTH_SHORT).show();
            else {
                FileOutputStream fileOutputStream = new FileOutputStream(picFile.getPath());
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                fileOutputStream.close();
                MediaStore.Images.Media.insertImage(this.getContentResolver(), bitmap, "", "");
                this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + picFile.getAbsolutePath())));
                //保存在数据库
                uploadMapMes(name);//图像站点信息
                uploadMap(name);           //图像尺寸、小车初始点
                //保存在手机端
                phoneUploadMapMes(name);
                //保存在服务器
//                uploadPicture(name);       //图像
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void phoneUploadMapMes(String picName){
        Gson gson=new Gson();
        List<DockSite>sendMesList=new ArrayList<>();
        for(DockSite dockSite:dockSites.values())
            sendMesList.add(dockSite);
        String mes=gson.toJson(sendMesList);//站点信息
        String mypic=gson.toJson(myPicture);//图像尺寸、小车初始点
        writeInPhone(picName,mes,mypic);
    }

    /**
     * 把图像保存到服务器里
     */
    private void uploadPicture(String pictureName){
        String url=baseUrl+"MyTest/UploadFileServlet";
        String filePath=Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/"+pictureName+".png";
        AsyncHttpClient httpClient=new AsyncHttpClient();
        RequestParams param=new RequestParams();
        try{
            File file=new File(filePath);
            param.put("file",file);
            httpClient.post(url, param, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                }
            });
        }catch (FileNotFoundException e){
            e.printStackTrace();
            Toast.makeText(Control_getPic.this,"不存在！",Toast.LENGTH_SHORT).show();
        }
    }
    private void uploadMapMes(String picName){
        Gson gson=new Gson();
        List<DockSite>sendMesList=new ArrayList<>();
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl(baseUrl)
                .build();
        //添加表
        UpMapMes upMapMes=retrofit.create(UpMapMes.class);
//        int count=0;
        for(DockSite dockSite:dockSites.values())
            sendMesList.add(dockSite);
        String sendMes=gson.toJson(sendMesList);
        Call<ResponseBody> data=upMapMes.uploadMes(picName,sendMes);
        data.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });

    }
    /**
     * 把图像相关数据保存到数据库里
     * 1. 图像名字、行、列、建立时间
     * 2. 站点x,y,angle
     */
    private void uploadMap(String picName){
        Gson gson=new Gson();
        String mypic=gson.toJson(myPicture);
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl(baseUrl)
                .build();
        UpMap upMap=retrofit.create(UpMap.class);
        Call<ResponseBody>data=upMap.upmap(picName,mypic);
        data.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String responseMes=response.body().string();
                    if(responseMes.equals("true"))
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                saveMapMesSuccess();
                            }
                        });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }

    public void writeInPhone(String fileName,String string1,String string2) {
        /**
         * 创建File对象，其中包含文件所在的目录以及文件的命名
         */
        File file = new File(Environment.getExternalStorageDirectory()+"/AgvCar"+"/mapMes",fileName+".txt");//
        // 创建FileOutputStream对象
        FileOutputStream outputStream = null;
        // 创建BufferedOutputStream对象
        BufferedOutputStream bufferedOutputStream = null;
        try {
            // 如果文件存在则删除
            if (file.exists()) {
                file.delete();
            }
            // 在文件系统中根据路径创建一个新的空文件
            file.createNewFile();
            // 获取FileOutputStream对象
            outputStream = new FileOutputStream(file);
            // 获取BufferedOutputStream对象
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            // 往文件所在的缓冲输出流中写byte数据
            bufferedOutputStream.write(string1.getBytes());
            bufferedOutputStream.write("\n".getBytes());
            bufferedOutputStream.write(string2.getBytes());
            // 刷出缓冲输出流，该步很关键，要是不执行flush()方法，那么文件的内容是空的。
            bufferedOutputStream.flush();
        } catch (Exception e) {
            // 打印异常信息
            e.printStackTrace();
        } finally {
            // 关闭创建的流对象
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    private void saveMapMesSuccess(){
        Toast.makeText(this,"地图信息成功保存",Toast.LENGTH_SHORT).show();
    }

    public static ImageView getImageView() {
        return c_imageView;
    }

    public static int getLx() {
        return Lx;
    }

    public static int getLy() {
        return Ly;
    }

    public static boolean isSendSpeedOrNot() {
        return sendSpeedOrNot;
    }

    public static boolean isStereoFlag() {
        return stereoFlag;
    }

    public void SavaImage(Bitmap bitmap, String path) {
        String picpath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/";
        File file = new File(picpath);
        if (!file.exists()) file.mkdirs();

        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        this.sendBroadcast(intent);
        try {
            FileOutputStream fileOutputStream = null;
            File picFile = new File(picpath, "aa.png");
            fileOutputStream = new FileOutputStream(picFile.getPath());
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.close();

            MediaStore.Images.Media.insertImage(this.getContentResolver(), bitmap, "", "");
            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + picFile.getAbsolutePath())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void LoadImage(){
        if(ContextCompat.checkSelfPermission(Control_getPic.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(Control_getPic.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
        else {
            openAlbum();
        }
    }
    private void openAlbum(){
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,CHOOSE_PHOTO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitkat(data);
                    } else {
                        handleImageBeforeKitkat(data);
                    }
                }
                break;
            default:
                break;
        }
    }
    @TargetApi(19)
    private void handleImageOnKitkat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果是document类型的uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content:" +
                        "//downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果是content类型的uri，则使用普通方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果是File类型的uri，直接获取图片路径即可
            imagePath = uri.getPath();
        }
        displayImage(imagePath);//根据图片路径显示图片
    }
    private void handleImageBeforeKitkat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);

    }
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        //通过uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }
    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            c_imageView.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
        }
    }


}
