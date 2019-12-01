package socket;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.along.agvcontrol0412.MainActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TcpQtClient implements Runnable{

    private int port;
    private String hostIP;
    private Socket socket;
    protected DataInputStream in;
    protected DataOutputStream out;
    private boolean connect = false;
    private boolean runFlag;

    private static TcpQtClient mTcpQtClient=null;
    private Handler handler =new Handler(Looper.getMainLooper());

    /**
     * 单例模式
     * @return
     */
    public static TcpQtClient getInstance(){
        if(mTcpQtClient==null){
            synchronized (TcpClient.class){
                if(mTcpQtClient==null){
                    mTcpQtClient=new TcpQtClient();
                }
            }
        }
        return mTcpQtClient;
    }
    public void connect(String hostIP, int port) {
        this.hostIP = hostIP;
        this.port = port;
        new Thread(this).start();
    }
    @Override
    public void run(){
        try {
            socket=new Socket(hostIP,port);
            runFlag=true;
            refreshUI(true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        in=new DataInputStream(socket.getInputStream());
                        out=new DataOutputStream(socket.getOutputStream());
                    }catch (IOException e){
                        e.printStackTrace();
                        runFlag=false;
                    }
                    while (runFlag) {
                        ;
                    }
                    try {
                        in.close();
                        out.close();
                        socket.close();

                        in = null;
                        out = null;
                        socket = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    connect = false;
                    refreshUI(false);
                }
            }).start();
            connect=true;
        }catch (Exception e){
            e.printStackTrace();

        }
    }
    public void stop() {
        runFlag = false;
        try {
            socket.shutdownInput();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean isConnected() {
        return connect;
    }

    private void refreshUI(final boolean isConnected){
        handler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.getEdPort2().setEnabled(!isConnected);  //端口
                MainActivity.getEdIp2().setEnabled(!isConnected);
                MainActivity.getBnQtConnect().setText(isConnected?"断开":"连接");
            }
        });
    }

    public void sendCoor(final float x, final float y, final float angle){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(out!=null){
                    try{
                        byte[] send=new byte[16];
                        send[0]=(byte)0xbb;
                        send[1]=(byte)0x01;
                        send[2]=(byte)0x02;
                        byte[] coorX=new byte[4];
                        byte[] coorY=new byte[4];
                        byte[] coorAngle=new byte[4];
                        FloatToByte(coorX,x);
                        FloatToByte(coorY,y);
                        FloatToByte(coorAngle,angle);
                        for(int i=0;i<4;i++){
                            send[3+i]=coorX[i];
                            send[7+i]=coorY[i];
                            send[11+i]=coorAngle[i];
                        }
                        send[15]=(byte)0x77;
                        OutputStream outputStream = socket.getOutputStream();
                        Log.i("tag","send success!!!");
                        outputStream.write(send);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    public void sendMap(final String imagePath){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int fileLength=0;
                    InputStream inputStream= null;
                    byte[] lenByte=new byte[4];
                    inputStream = new FileInputStream(imagePath);
                    fileLength=inputStream.available();
                    lenByte=intToByte(fileLength);//计算图片文件长度
                    byte[] data=new byte[fileLength];
                    //读取图片数据
                    int offset=0;
                    while(offset<fileLength){
                        int count=inputStream.read(data,offset,fileLength-offset);
                        offset+=count;
                    }
                    inputStream.close();
                    byte[] send=new byte[fileLength+8];
                    send[0]=(byte)0xbb;
                    send[1]=(byte)0x01;
                    send[2]=(byte)0x03;
                    for(int i=0;i<4;i++)
                        send[3+i]=lenByte[i];
                    for(int i=0;i<fileLength;i++)
                        send[7+i]=data[i];
                    send[fileLength+7]=(byte)0x77;
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(send);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void sendMap(final String imagePath,final float x, final float y, final float angle){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(out!=null){
                    try{
                        int fileLength=0;
                        InputStream inputStream= null;
                        byte[] lenByte=new byte[4];
                        inputStream = new FileInputStream(imagePath);
                        fileLength=inputStream.available();
                        lenByte=intToByte(fileLength);//计算图片文件长度
                        byte[] data=new byte[fileLength];
                        //读取图片数据
                        int offset=0;
                        while(offset<fileLength){
                            int count=inputStream.read(data,offset,fileLength-offset);
                            offset+=count;
                        }
                        inputStream.close();

                        byte[] coorX=new byte[4];
                        byte[] coorY=new byte[4];
                        byte[] coorAngle=new byte[4];
                        FloatToByte(coorX,x);
                        FloatToByte(coorY,y);
                        FloatToByte(coorAngle,angle);


                        byte[] send=new byte[3+4+12+fileLength+1];
                        send[0]=(byte)0xbb;
                        send[1]=(byte)0x01;
                        send[2]=(byte)0x02;
                        for(int i=0;i<4;i++){
                            send[3+i]=lenByte[i];
                            send[7+i]=coorX[i];
                            send[11+i]=coorY[i];
                            send[15+i]=coorAngle[i];
                        }
                        for(int i=0;i<fileLength;i++)
                            send[19+i]=data[i];
                        send[3+4+12+fileLength]=(byte)0x77;
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(send);
                        Log.i("tag","send success!!_____");

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    public void sendMap(final byte[] map, final int width, final int height, final float x, final float y, final float angle){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(out!=null){
                    try{
                        int length=map.length+24;
                        byte[] send=new byte[length];
                        send[0]=(byte)0xbb;
                        send[1]=(byte)0x01;
                        send[2]=(byte)0x01;
                        byte[] widthByte=new byte[4];
                        byte[] heightByte=new byte[4];
                        byte[] xByte=new byte[4];
                        byte[] yByte=new byte[4];
                        byte[] angleByte=new byte[4];
                        widthByte=intToByte(width);
                        heightByte=intToByte(height);
                        FloatToByte(xByte,x);
                        FloatToByte(yByte,y);
                        FloatToByte(angleByte,angle);
                        for(int i=0;i<4;i++){
                            send[3+i]=widthByte[i];
                            send[7+i]=heightByte[i];
                            send[11+i]=xByte[i];
                            send[15+i]=yByte[i];
                            send[19+i]=angleByte[i];
                        }
                        for(int i=0;i<map.length;i++)
                            send[23+i]=map[i];
                        send[length-1]=(byte)0x77;
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(send);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    private static byte[] intToByte(int val){
        byte[] b = new byte[4];
        b[0] = (byte)(val & 0xff);
        b[1] = (byte)((val >> 8) & 0xff);
        b[2] = (byte)((val >> 16) & 0xff);
        b[3] = (byte)((val >> 24) & 0xff);
        return b;
    }
    private static void FloatToByte(byte[] bb, float x) {
        // byte[] b = new byte[4];
        int l = Float.floatToIntBits(x);
        for (int i = 0; i < 4; i++) {
            bb[i] = new Integer(l).byteValue();
            l = l >> 8;
        }
    }
}
