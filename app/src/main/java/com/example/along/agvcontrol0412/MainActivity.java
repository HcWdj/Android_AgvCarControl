package com.example.along.agvcontrol0412;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import MyClass.DockSite;
import socket.TcpClient;
import socket.TcpQtClient;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private String imagePath;//图片路径
    //***变量***//
    private static boolean receive_flag=false;
    private static boolean sendPicOrNot=false;
    private static boolean navigateOrNot=false;
    //***连接ip、端口用***//
    private static Button bnConnect,bnQtConnect,bnSend;     //连接、发送
    private static EditText edIp,edPort,edIp2,edPort2,edData; //IP地址、端口号、输入数据 文本框
    //***跳转界面***//
    private static Button bnBuildPic,bnNavigation;           //开始遥控并显示图像
    //textview
    public static TextView tv_voltage;
    //***多线程***//
    private Handler handler =new Handler(Looper.getMainLooper());
    //***单例模式***//
    private TcpClient client=TcpClient.getInstance();
    private TcpQtClient qtClient=TcpQtClient.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //***对接xml***//
        bnConnect=this.findViewById(R.id.bn_connect);
        bnQtConnect=this.findViewById(R.id.bn_connect2);

        edIp=this.findViewById(R.id.ed_ip);
        edPort=this.findViewById(R.id.ed_port);
        edIp2=this.findViewById(R.id.ed_ip2);
        edPort2=this.findViewById(R.id.ed_port2);
        //edData=this.findViewById(R.id.ed_dat);
        bnBuildPic=this.findViewById(R.id.bn_buildPic);
        bnNavigation=this.findViewById(R.id.bn_navigation);

        tv_voltage=this.findViewById(R.id.main_batteryVoltage);


        //***监听***//
        bnConnect.setOnClickListener(this);
        bnQtConnect.setOnClickListener(this);
        bnBuildPic.setOnClickListener(this);
        bnNavigation.setOnClickListener(this);

        refreshUI(false);  //刷新界面
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        if(receive_flag || navigateOrNot){  //满足其中一种情况
        //if(sendPicOrNot || navigateOrNot){
            client.endPic();//(发送 停止发图的指令)现在的目的是让小车端退出相应的模式
        }
        receive_flag=false;
        sendPicOrNot=false; //建图模式结束
        navigateOrNot=false;//导航模式结束
        client.setMybitmap(null);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.bn_connect:
                connect();
                break;
            case R.id.bn_connect2:
                qtConnect();
                break;
            case R.id.bn_buildPic:
                Intent BuildPic=new Intent(MainActivity.this,Control_getPic.class);
                receive_flag=true;  //允许接受标志位
                sendPicOrNot=true;  //允许接受图像 进入Control_getpic时发送指令，退出时，标为false
                //client.AskForPic(); //发送接收图像请求（改到点击“开始建图”的时候开启）
                client.sendSpeed(); //发送速度线程
                startActivity(BuildPic);
                break;
            case R.id.bn_navigation:
                Intent navigation=new Intent(MainActivity.this,Navigation.class);
                navigateOrNot=true;
                client.AskForNavi();
                startActivity(navigation);
                break;
        }
    }

    private void connect(){
        if(client.isConnected()){
            client.stop();
        } else {
            try {
                String hostIP = edIp.getText().toString();               //获取App里写的IP地址
                int port = Integer.parseInt(edPort.getText().toString());//获取APP里写的端口号，string->int
                client.connect(hostIP, port);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "端口错误", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
    private void qtConnect(){
        if(qtClient.isConnected()){
            qtClient.stop();
        }else{
            try{
                String hostIp= edIp2.getText().toString();
                int port=Integer.parseInt(edPort2.getText().toString());
                qtClient.connect(hostIp,port);
            }catch (NumberFormatException e){
                Toast.makeText(this, "端口错误", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void refreshUI(final boolean isConnected){
        handler.post(new Runnable() {
            @Override
            public void run() {
                edPort.setEnabled(!isConnected);  //端口
                edIp.setEnabled(!isConnected);
                bnConnect.setText(isConnected?"断开":"连接");
            }
        });
    }

    public static Button getBnConnect() {
        return bnConnect;
    }

    public static EditText getEdIp() {
        return edIp;
    }

    public static EditText getEdPort() {
        return edPort;
    }

    public static Button getBnQtConnect() {
        return bnQtConnect;
    }

    public static EditText getEdIp2() {
        return edIp2;
    }

    public static EditText getEdPort2() {
        return edPort2;
    }

    public static boolean isReceive_flag() {
        return receive_flag;
    }

    public static void setReceive_flag(boolean receive_flag) {
        MainActivity.receive_flag = receive_flag;
    }

    public static boolean isNavigateOrNot() {
        return navigateOrNot;
    }

}
