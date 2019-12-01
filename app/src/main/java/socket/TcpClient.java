package socket;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import com.example.along.agvcontrol0412.Control_getPic;
import com.example.along.agvcontrol0412.MainActivity;
import com.example.along.agvcontrol0412.Navigation;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import MyClass.DockSite;
import MyClass.StereoPoint;
import MyClass.NavigationMessage;
import recev.BatteryPower;
import recev.GoalAchieve;
import recev.Map_xy;
import recev.Save_Map_success;
import send.AskForNavigation;
import send.AskForPbstream;
import send.AskForPic;
import send.Charge;
import send.EndPic;
import send.Map_xy_send;
import send.NavigationMap;
import send.RecordCharge;
import send.SaveMap;
import send.SendSetSpeed;
import send.StopCharge;
import send.TargetPoint;

/**
 *    head   cmd   len   data  back
 *    AA      ..                77
 *
 *
 *    cmd   data
 *     01 -  00   遥控模式下停止发送图片
 *           01   请求遥控建图
 *           02   请求导航全图
 *           03
 *           04
 *     02         摇杆
 *     03         设定速度
 *     04         手指点的坐标
 *     05    01   保存图片         //功能
 *           02   保存充电桩坐标
 *           03   停止前往充电桩
 *           04   请求发送pbstream文件
 *           05   记录充电桩点云模版
 *     40         小车用来导航的图
 *
 *
 *
 *     80         获取当前地图点
 *     81         保存地图成功反馈(已不用)
 *     82    01   到达目的点
 *     83         电量
 *     84         vslam状态
 */


public class TcpClient implements Runnable{
    private Bitmap mybitmap;
    private Socket socket;
    private int port;
    private String hostIP;
    private boolean connect = false;
    private boolean runFlag;
    private boolean pbstreamFlag=true;
    private boolean finish_recevPic=false;
    private boolean saveMapSuccessFlag=false;
    private boolean achieveGoalFlag=true;//默认已经到达位置，每次发指令的时候置false
    private int picW=0,picH=0;
    private float currentX=0,currentY=0,currentAngle=0;
    private float original_pointX,original_pointY,original_pointAngle=0;
    private float charge_pointX=0,charge_pointY=0,charge_pointAngle=0;
    private float pi= (float) 3.1415926;
    private float voltage=0;
    private int status=0;
    protected DataInputStream in;
    protected DataOutputStream out;
    byte[] picLenBuff = new byte[20];
    byte[] recevBuff=new byte[3];
    byte[] pbstreamHead=new byte[2];
    byte[] batteryPowerHead=new byte[2];
    byte[] batteryPowerBuff=new byte[9];
    private byte[] editBuff;
    private int count=0;
    private int tempcount=0;
    //------------Double Eye-------------------
    private ArrayList<StereoPoint>stereoPoints=new ArrayList<>();
    private int stereoWidth;
    private int stereoHeight;
    private int maxStereoWidth=0;
    private int maxStereoHeight=0;
    private int vslamStatus=0;
    //-----------------------------------------
    private Handler handler =new Handler(Looper.getMainLooper());

    private Map_xy_send map_xy_send=Map_xy_send.getInstance_map_xy_send();
    private Map_xy map_xy=Map_xy.getInstance_map_xy();
    private GoalAchieve goalAchieve=GoalAchieve.getInstance_goal_achieve();
    private TargetPoint targetPoint=TargetPoint.getInstance_TargetPoint();
    private AskForNavigation askForNavigation=AskForNavigation.getInstance_AskForNavigation();
    private SendSetSpeed sendSetSpeed=SendSetSpeed.getInstance_SendSetSpeed();
    private Save_Map_success save_map_success=Save_Map_success.getInstance();
    private NavigationMap navigationMap=NavigationMap.getInstance_navigationMap();
    private NavigationMessage navigationMessage=NavigationMessage.getInstance_navigationMessage();
    private BatteryPower batteryPower=BatteryPower.getInstance_batteryPower();
    private EndPic endPic=new EndPic();
    private AskForPic askForPic=new AskForPic();
    private SaveMap saveMap=new SaveMap();
    private Charge mcharge=new Charge();
    private StopCharge stopCharge=new StopCharge();
    private AskForPbstream askForPbstream=new AskForPbstream();
    private RecordCharge recordCharge=new RecordCharge();

    private TcpQtClient qtClient=TcpQtClient.getInstance();
    private static TcpClient mTcpClient=null;
    /**
     * 单例模式
     * @return
     */
    public static TcpClient getInstance(){
        if(mTcpClient==null){
            synchronized (TcpClient.class){
                if(mTcpClient==null){
                    mTcpClient=new TcpClient();
                }
            }
        }
        return mTcpClient;
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
                    while (runFlag){
                        //------------------stereo--------------------
                        if(Control_getPic.isStereoFlag()) {
                            StereoPoint();  //接收双目坐标
                            receiveStatus();//接收双目状态
                        }
                        //---------------------------------------------
                        receive_battery_power();

                        //------------------stereo--------------------
                        if(!Control_getPic.isStereoFlag())
                        //---------------------------------------------
                        if(MainActivity.isReceive_flag()) {
                            //Log.i("建图模式中","success");
                            ctl_showpic();
//                            if(voltage!=0)
//                                Control_getPic.tv_voltage.setText("状态:"+status+"   电池电压:"+voltage+"V");
                        }
//                        if(!MainActivity.isReceive_flag() && !MainActivity.isNavigateOrNot()){
//                            ctl_recevPbstream();
//                        }
//                        if(MainActivity.isNavigateOrNot()){
//                            //Log.i("导航模式中","success");
//                            navigation_show();
////                            if(voltage!=0)
////                                Navigation.tv_voltage.setText("状态:"+status+"   电池电压:"+voltage+"V");
//                        }
                    }
                    try {
                        in.close();
                        out.close();
                        socket.close();

                        in = null;
                        out = null;
                        socket = null;
                        mybitmap=null;
                        navigationMessage.clearMessage();
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

    private void receiveStatus(){
        try{

            in.read(recevBuff,0,3);
            if(recevBuff[0]==(byte)0xaa && recevBuff[1]==(byte)0x84 && recevBuff[2]==1){
                byte[] temp=new byte[2];
                in.read(temp,0,2);
                if(temp[1]==(byte)0x77) {
                    vslamStatus = temp[0];
                    Control_getPic.tv_voltage.setText("状态:"+vslamStatus);
                    Log.i("tag","状态:"+vslamStatus);
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void StereoPoint(){
        try{
            in.read(recevBuff,0,3);
            if(recevBuff[0]==((byte)0xaa) && recevBuff[1]==map_xy.getCmd() && recevBuff[2]==map_xy.getLength()){//机器人坐标
                byte[] localxy=new byte[12];
                byte[] back=new byte[1];
                in.read(localxy, 0, 12);
                in.read(back,0,1);
                if((int)back[0]==(int)map_xy.getBack()) {
                    currentX = byteToFloat(Arrays.copyOfRange(localxy, 0, 4));
                    currentY = byteToFloat(Arrays.copyOfRange(localxy, 4, 8));
                    currentAngle=byteToFloat(Arrays.copyOfRange(localxy, 8, 12));
                    map_xy.set(currentX,currentY,currentAngle*180/pi);
//                    if(stereoPoints.size()>100)
                    stereoPoints.add(new StereoPoint(currentX,currentY));
                    Log.i("tag","x="+currentX+",y="+currentY+",angle="+currentAngle);

                    stereoWidth=(int)Math.abs(400*currentX);
                    stereoHeight=(int)Math.abs(400*currentY);

                    if(maxStereoHeight<stereoHeight) maxStereoHeight=stereoHeight;
                    if(maxStereoWidth<stereoWidth) maxStereoWidth=stereoWidth;

//                    maxStereoHeight=1500;
//                    maxStereoWidth=1500;

                    byte[] bitmapbuff=new byte[maxStereoHeight*maxStereoWidth];
                    for(int i=0;i<maxStereoHeight*maxStereoWidth;i++)
                        bitmapbuff[i]=0;
                    mybitmap = createBitmap(bitmapbuff, maxStereoWidth, maxStereoHeight);
                    Log.i("tag","width="+maxStereoWidth+",height="+maxStereoHeight);
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(mybitmap!=null)
                            drawStereo(map_xy.getX(), map_xy.getY(), map_xy.getAngle(),maxStereoWidth,maxStereoHeight,mybitmap);

                    }
                });
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private void drawStereo(float x,float y,float angle,int stereoW,int stereoH,Bitmap bitmap) {
        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();

        int r=(int)Math.min(stereoW,stereoH)/40;

        paint.setColor(Color.GREEN);
        for(StereoPoint stereoPoint :stereoPoints){
            canvas.drawCircle(stereoW-stereoW/2+stereoPoint.x*100,stereoH/2+stereoPoint.y*100,r,paint);
        }
        paint.setColor(Color.RED);
        canvas.drawCircle(stereoW-stereoW/2+x*100,stereoH/2+y*100,r,paint);


        Log.i("tag","--->"+stereoPoints.size());


        ImageView view = Control_getPic.getImageView();
        view.setImageBitmap(bmp);
    }

    /**
     * 接收pbstream文件
     */
    private void ctl_recevPbstream(){
        try {
            in.read(recevBuff,0,3);//此处第三位为长度，自动略过，但还是接收了
            if(recevBuff[0]==((byte)0xaa) && recevBuff[1]==((byte)0xfd) && recevBuff[2] == 0){//pbstream文件
                byte[] pbstreamLength=new byte[4];
                in.read(pbstreamLength, 0, 4);
                int pbstreamLen = byteArrayToInt(Arrays.copyOfRange(pbstreamLength, 0, 4));
                int offset = 0;
                byte[] pbstreamBuff = new byte[pbstreamLen];
                byte[] temp=new byte[1];
                while (offset < pbstreamLen) {
                    int fileL = in.read(pbstreamBuff, offset, pbstreamLen - offset);
                    offset += fileL;
                }
                in.read(temp,0,1);//帧尾
                if((int)temp[0]==(int)0x77){
                    Control_getPic.createFileWithByte(pbstreamBuff);
                    pbstreamFlag=true;
                    Control_getPic.tv_voltage.setText("保存成功");
                }
//                    else if(pbstreamFlag==false){
//                        AskForPbstream();
//                    }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void receivePbstream(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pbstreamFlag=false;
                    while(!pbstreamFlag) {
                        AskForPbstream();
                        Control_getPic.tv_voltage.setText("请求中.." + tempcount++);
                        Thread.sleep(2000);
                    }
                    if (pbstreamFlag)
                        tempcount = 0;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 接收电量
     */
    private void receive_battery_power(){
        try{
            in.read(batteryPowerHead,0,2);
//            Log.i("tag","[0]="+batteryPowerHead[0]+",[1]="+batteryPowerHead[1]);
            if(batteryPowerHead[0]==(byte)0xaa && batteryPowerHead[1]==(byte)0x83){
                byte[] len=new byte[1];
                in.read(len,0,1);
                if(len[0]==8){
                    in.read(batteryPowerBuff,0,9);
                    if(batteryPowerBuff[8]==(byte)0x77){
                        byte[] status_byte=new byte[4];
                        byte[] power_byte=new byte[4];
                        for(int i=0;i<4;i++) {
                            power_byte[i] = batteryPowerBuff[4 + i];
                            status_byte[i]=batteryPowerBuff[i];
                        }
                        voltage=byteToFloat(power_byte);
                        DecimalFormat df = new DecimalFormat("########.00");
                        voltage=Float.parseFloat(df.format(voltage));
                        status=byteArrayToInt(status_byte);

                        batteryPower.set(status,voltage);
                        Log.i("电量","状态："+status+",电压："+voltage);
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 建图模式 1.接受图像 again and again 2.显示当前点
     */
    private void ctl_showpic(){
        try {
            /**
             * 获取相关信息
             */
            in.read(recevBuff,0,3);//此处第三位为长度，自动略过，但还是接收了
            if(recevBuff[0]==((byte)0xaa) && recevBuff[1]==((byte)0xff)) {//地图
                in.read(picLenBuff, 0, 20);
                picW = byteArrayToInt(Arrays.copyOfRange(picLenBuff, 0, 4));
                picH = byteArrayToInt(Arrays.copyOfRange(picLenBuff, 4, 8));
                original_pointX=byteToFloat(Arrays.copyOfRange(picLenBuff, 8, 12));
                original_pointY=byteToFloat(Arrays.copyOfRange(picLenBuff, 12, 16));
                original_pointAngle=byteToFloat(Arrays.copyOfRange(picLenBuff, 16, 20));

                /**
                 * 接收图像
                 */
                int offset = 0;
                final byte[] bitmapbuff = new byte[picW * picH];
                byte[] temp=new byte[1];
                while (offset < picW * picH) {
                    int picL = in.read(bitmapbuff, offset, picW * picH - offset);
                    offset += picL;
                }
                in.read(temp,0,1);//帧尾

                /**
                 * 判断帧尾  如果相同，接受图像
                 */
                if((int)temp[0]==(int)0x77){
                    editBuff=bitmapbuff;//存储作编辑的时候用
                    mybitmap = createBitmap(bitmapbuff, picW, picH);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            qtClient.sendMap(bitmapbuff,picW,picH,currentX,currentY,currentAngle);
                            Control_getPic.getImageView().setImageBitmap(mybitmap);
                            draw_current_point(map_xy.getX(), map_xy.getY(), map_xy.getAngle(),mybitmap);
                            Log.i("TcpClient","建图模式接受图像success!");
                        }
                    });
                }
            }
            else if(recevBuff[0]==((byte)0xaa) && recevBuff[1]==map_xy.getCmd() && recevBuff[2]==map_xy.getLength()){//机器人坐标
                byte[] localxy=new byte[12];
                byte[] back=new byte[1];
                in.read(localxy, 0, 12);
                in.read(back,0,1);
                if((int)back[0]==(int)map_xy.getBack()) {
                    currentX = byteToFloat(Arrays.copyOfRange(localxy, 0, 4));
                    currentY = byteToFloat(Arrays.copyOfRange(localxy, 4, 8));
                    currentAngle=byteToFloat(Arrays.copyOfRange(localxy, 8, 12));
                    map_xy.set(currentX,currentY,currentAngle*180/pi);
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Control_getPic.getImageView().setImageBitmap(mybitmap);
                        if(mybitmap!=null)
                             draw_current_point(map_xy.getX(), map_xy.getY(), map_xy.getAngle(),mybitmap);
                    }
                });
            }
        } catch (IOException e) {
            runFlag = false;
        }
    }

    /**
     * 导航模式： 1.显示地图 2.显示当前点 3.显示目标点
     */
    private void navigation_show(){
        try {
            /**
             * 获取相关信息
             */
            in.read(recevBuff,0,3);
            if(recevBuff[0]==((byte)0xaa) && recevBuff[1]==map_xy.getCmd() && recevBuff[2]==map_xy.getLength()){//机器人坐标
                byte[] localxy=new byte[12];
                byte[] back=new byte[1];
                in.read(localxy, 0, 12);
                in.read(back,0,1);
                if((int)back[0]==(int)map_xy.getBack()) {
                    currentX = byteToFloat(Arrays.copyOfRange(localxy, 0, 4));
                    currentY = byteToFloat(Arrays.copyOfRange(localxy, 4, 8));
                    float angle=byteToFloat(Arrays.copyOfRange(localxy, 8, 12));
                    map_xy.set(currentX,currentY,angle*180/pi);
                }
                if(finish_recevPic) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Navigation.getImageView().setImageBitmap(mybitmap);
//                            qtClient.sendCoor(currentX,currentY,currentAngle);
//                            qtClient.sendMap(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/"+navigationMessage.getImagePath()+".png");
                            String path=Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/"+navigationMessage.getImagePath()+".png";
                            count++;
//                            Log.i("tag","___________count="+count);
                            if(count==8){
                                count=0;
                                qtClient.sendMap(path,currentX,currentY,currentAngle);
//                                Log.i("tag","currentX="+currentX+",currentY"+currentY+",currentAngle"+currentAngle);
                            }

                            if(mybitmap!=null)
                                draw_point(map_xy.getX(),map_xy.getY(), map_xy.getAngle(), map_xy_send.getX(), map_xy_send.getY(),map_xy_send.getAngle(), mybitmap);
                        }
                    });
                }
            }else if(recevBuff[0]==goalAchieve.getHead() && recevBuff[1]==goalAchieve.getCmd() && recevBuff[2]==goalAchieve.getLength()){
                byte[] data=new byte[1];//到达目的点
                byte[] back=new byte[1];
                in.read(data,0,1);
                in.read(back,0,1);
                if(back[0]==goalAchieve.getBack() && data[0]==goalAchieve.getData()){
                    if(Navigation.getTaskChainItem().size()!=0){//如果数组容量不为0，说明执行的是任务链
                       delay(Navigation.getTaskChainItem().get(0).getDelay());
                    }else {//如果容量为0，说明执行的是单个
                        Navigation.tv_status.setText("当前无任务");
                        achieveGoalFlag = true;
                    }
                }
            }
        } catch (IOException e) {
            runFlag = false;
        }
    }

    private void delay(final int time){
        //延时结束的时候，标志位改变，任务列表改变
        //延时中，显示延时时间
        TimerTask task=new TimerTask() {
            int delayTime=time;
            @Override
            public void run() {
                Navigation.tv_status.setText("等待"+delayTime+"秒");
                delayTime--;
                if(delayTime==-1) {
                    Navigation.tv_status.setText("当前无任务");
                    Navigation.removeTaskChainItem();
                    achieveGoalFlag = true;
                    this.cancel();
                }
            }
        };
        Timer timer2=new Timer();
        timer2.scheduleAtFixedRate(task,0,1000);
    }

    public void saveMapOrNot(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (out != null) {
                    try {
                        in.read(recevBuff, 0, 3);
                        if (recevBuff[0] == save_map_success.getHead() && recevBuff[1] == save_map_success.getCmd() && recevBuff[2] == save_map_success.getLength()) {
                            byte[] saveMapTemp = new byte[2];
                            in.read(saveMapTemp, 0, 2);
                            if (saveMapTemp[0] == save_map_success.getData() && saveMapTemp[1] == save_map_success.getBack()) {
                                saveMapSuccessFlag = true;
                                Log.i("save Map","success"+saveMapSuccessFlag);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    /**
     * 发送摇杆
     */
    public void sendSpeed(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (out != null) {
                    try {
                        while(true) {
                            byte speed_local[] = new byte[6];
                            speed_local[0] = (byte)0xaa;
                            speed_local[1] = (byte)0x02;
                            speed_local[2] = (byte)2;
                            speed_local[3] = (byte) Control_getPic.getLx();
                            speed_local[4] = (byte) Control_getPic.getLy();
                            speed_local[5] = (byte)0x77;
                            OutputStream outputStream = socket.getOutputStream();
                            outputStream.write(speed_local);
                            Thread.sleep(20);//20ms
                            //Log.i("TcpClient--sendSpeed","Success!!");
                            if(!Control_getPic.isSendSpeedOrNot())
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    public void relocationSendSpeed(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(out!=null){
                    try{
                        while(true){
                            byte speed_local[] = new byte[6];
                            speed_local[0] = (byte)0xaa;
                            speed_local[1] = (byte)0x02;
                            speed_local[2] = (byte)2;
                            speed_local[3] = (byte) Navigation.getLx();
                            speed_local[4] = (byte) Navigation.getLy();
                            speed_local[5] = (byte)0x77;
                            OutputStream outputStream = socket.getOutputStream();
                            outputStream.write(speed_local);
                            Thread.sleep(20);//20ms
                            if(!Navigation.isRelocationSendSpeed())
                                break;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    /**
     * 请求导航
     */
    public void AskForNavi(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                    if(out!=null){
                        try{
//                            while(true) {
                                byte send[] = new byte[5];
                                send[0] = askForNavigation.getHead();
                                send[1] = askForNavigation.getCmd();
                                send[2] = askForNavigation.getLength();
                                send[3] = askForNavigation.getData();
                                send[4] = askForNavigation.getBack();
                                OutputStream outputStream = socket.getOutputStream();
                                outputStream.write(send);
                                Thread.sleep(100);//ms
                                Log.i("ask for navigation img", "success");

//                                if (finish_recevPic)
//                                    break;
//                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
            }
        }).start();
    }

    /**
     * 请求图像
     */

    public void AskForPic(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (out != null) {
                    try {
                        byte send[] = new byte[5];
                        send[0] = askForPic.getHead();
                        send[1] = askForPic.getCmd();
                        send[2] = askForPic.getLength();
                        send[3] = askForPic.getData();
                        send[4] = askForPic.getBack();
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(send);
                        MainActivity.setReceive_flag(true);
                        Log.i("ask for picture","success");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    /**
     * 请求pbstream文件
     */

    public void AskForPbstream(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (out != null) {
                    try {
                        byte send[] = new byte[5];
                        send[0] = askForPbstream.getHead();
                        send[1] = askForPbstream.getCmd();
                        send[2] = askForPbstream.getLength();
                        send[3] = askForPbstream.getData();
                        send[4] = askForPbstream.getBack();
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(send);
                        Log.i("ask for pbstream","success");
                        pbstreamFlag=false;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    /**
     * 保存图像
     */
    public void SaveMap(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (out != null) {
                    try {
                        byte send[] = new byte[5];
                        send[0] = saveMap.getHead();
                        send[1] = saveMap.getCmd();
                        send[2] = saveMap.getLength();
                        send[3] = saveMap.getData();
                        send[4] = saveMap.getBack();
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(send);
                        Log.i("Save Map instruct","success");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    /**
     * 结束发送图像
     */
    public void endPic(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (out != null) {
                    try {
                        byte send[] = new byte[5];
                        send[0] = endPic.getHead();
                        send[1] = endPic.getCmd();
                        send[2] = endPic.getLength();
                        send[3] = endPic.getData();
                        send[4] = endPic.getBack();
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(send);
                        MainActivity.setReceive_flag(false);
                        Log.d("refuse picture","success");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 导航模式发送指定目标点 未用到
     */
    public void sendTarget(){//sendMapLocation
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (out != null) {
                    try {
                        byte[] targetX = new byte[4];
                        byte[] targetY = new byte[4];
                        byte[] sendTargetPoint = new byte[4 + targetPoint.getLength()];
                        FloatToByte(targetX, targetPoint.getX());//width
                        FloatToByte(targetY, targetPoint.getY());//height
                        sendTargetPoint[0] = targetPoint.getHead();
                        sendTargetPoint[1] = targetPoint.getCmd();
                        sendTargetPoint[2] = targetPoint.getLength();
                        for (int temp = 0; temp < 4; temp++) {
                            sendTargetPoint[3 + temp] = targetX[temp];
                            sendTargetPoint[7 + temp] = targetY[temp];
                        }
                        sendTargetPoint[3 + targetPoint.getLength()] = targetPoint.getBack();

                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(sendTargetPoint);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 发送设定速度
     * @param
     */
    public void sendSetSpeed() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (out != null) {
                    try {
                        byte[] lineSpeed = new byte[4];
                        byte[] angleSpeed = new byte[4];
                        byte[] sendSetspeed = new byte[4 + sendSetSpeed.getLength()];
                        FloatToByte(lineSpeed, sendSetSpeed.getLineSpeed());//width
                        FloatToByte(angleSpeed, sendSetSpeed.getAngleSpeed());//height
                        sendSetspeed[0] = sendSetSpeed.getHead();
                        sendSetspeed[1] = sendSetSpeed.getCmd();
                        sendSetspeed[2] = sendSetSpeed.getLength();
                        for (int temp = 0; temp < 4; temp++) {
                            sendSetspeed[3 + temp] = lineSpeed[temp];
                            sendSetspeed[7 + temp] = angleSpeed[temp];
                        }
                        sendSetspeed[3 + sendSetSpeed.getLength()] = sendSetSpeed.getBack();
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(sendSetspeed);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 发送小车目标坐标
     */
    public void sendMapLocation(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    achieveGoalFlag=false;
                    byte[] send_width=new byte[4];
                    byte[] send_height=new byte[4];
                    byte[] send_angle=new byte[4];

                    byte[] mapLocation_data=new byte[4+map_xy_send.getLength()];
                    FloatToByte(send_width,map_xy_send.getX());
                    FloatToByte(send_height,map_xy_send.getY());
                    FloatToByte(send_angle,map_xy_send.getAngle());
                    Log.i("要发送的点","x="+map_xy_send.getX()+",y="+map_xy_send.getY());
                    mapLocation_data[0]=map_xy_send.getHead();
                    mapLocation_data[1]=map_xy_send.getCmd();
                    mapLocation_data[2]=map_xy_send.getLength();
                    for(int temp=0;temp<4;temp++){
                        mapLocation_data[3+temp]=send_width[temp];
                        mapLocation_data[7+temp]=send_height[temp];
                        mapLocation_data[11+temp]=send_angle[temp];
                    }
                    mapLocation_data[3+map_xy_send.getLength()]=map_xy_send.getBack();
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(mapLocation_data);

//                    Log.i("充电","x="+map_xy_send.getX()+",y="+map_xy_send.getY()+",angle="+map_xy_send.getAngle());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void goCharging(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] send_chargeX=new byte[4];
                    byte[] send_chargeY=new byte[4];
                    byte[] send_chargeAngle=new byte[4];

                    byte[] chargeLocation=new byte[4+map_xy_send.getLength()];

                    FloatToByte(send_chargeX,map_xy_send.getX());
                    FloatToByte(send_chargeY,map_xy_send.getY());
                    FloatToByte(send_chargeAngle,map_xy_send.getAngle());



                    chargeLocation[0]=map_xy_send.getHead();
                    chargeLocation[1]=map_xy_send.getCmd();
                    chargeLocation[2]=map_xy_send.getLength();

                    for(int temp=0;temp<4;temp++){
                        chargeLocation[3+temp]=send_chargeX[temp];
                        chargeLocation[7+temp]=send_chargeY[temp];
                        chargeLocation[11+temp]=send_chargeAngle[temp];
                    }
                    chargeLocation[3+map_xy_send.getLength()]=map_xy_send.getBack();
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(chargeLocation);

                    Log.i("充电","x="+map_xy_send.getX()+",y="+map_xy_send.getY()+",angle="+map_xy_send.getAngle());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void chargefunc(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    byte speed_local[] = new byte[5];
                    speed_local[0] = mcharge.getHead() ;
                    speed_local[1] = mcharge.getCmd();
                    speed_local[2] = mcharge.getLength();
                    speed_local[3] = mcharge.getData();
                    speed_local[4] = mcharge.getBack();
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(speed_local);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /**
     * 发送图像给小车
     */
    public void sendMapToCar(final String imagePath){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int fileLength=0;
                    byte[] pbstreamLen=new byte[4];//pbstream长度
                    byte[] lenByte=new byte[4];
                    byte[] originX=new byte[4];
                    byte[] originY=new byte[4];
                    FloatToByte(originX,navigationMap.getOrigin_x());
                    FloatToByte(originY,navigationMap.getOrigin_y());
                    InputStream inputStream=new FileInputStream(imagePath);
                    fileLength=inputStream.available();
                    lenByte=intToByte(fileLength);//计算图片文件长度
                    byte[] data=new byte[fileLength];
                    //读取图片数据
                    int offset=0;
                    while(offset<fileLength){
                        int count=inputStream.read(data,offset,fileLength-offset);
                        offset+=count;
                    }
//                    inputStream.read(data);
                    inputStream.close();
                    //读取pbstream数据
                    byte[] pbstreamBuff=Control_getPic.getByteStream(Navigation.getImagePath());
                    pbstreamLen=intToByte(pbstreamBuff.length);
                    //确定发送类的数据长度 dataLength
                    navigationMap.setLength(fileLength);
                    //设置发送数组,总长度:head=1,cmd=1,len=4,originX=4,originY=4,data=getLength(data=available),back=1
                    byte[]send=new byte[7+4+4+4+navigationMap.getLength()+pbstreamBuff.length];//
                    send[0]=navigationMap.getHead();    //0:head
                    send[1]=navigationMap.getCmd();     //1:cmd
                    for(int temp=0;temp<4;temp++) {
                        send[2 + temp] = lenByte[temp];     //2-5:length
                        send[6 + temp] = pbstreamLen[temp];  //6-9: pbstream
                        send[10 + temp] = originX[temp];     //10-13:origin x
                        send[14 + temp] = originY[temp];    //14-17:origin y
                    }
                    for(int temp=0;temp<fileLength;temp++)
                        send[18+temp]=data[temp];
                    for(int temp=0;temp<pbstreamBuff.length;temp++)
                        send[18+fileLength+temp]=pbstreamBuff[temp];
                    send[18+fileLength+pbstreamBuff.length]=navigationMap.getBack();//
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(send);
                    Log.i("TAG","pbstreamlength"+pbstreamBuff.length);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
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

    /**
     * 画当前点
     */
    public void draw_current_point(float lx,float ly,float angle,Bitmap bitmap) {
        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();

        int x,y;
        x=(int)(lx*100/5);
        y=picH-(int)(ly*100/5);

        //画一个带方向的三角形
        canvas.rotate(-angle,x,y);
        Path path=new Path();
        path.moveTo(x+5,y);
        path.lineTo(x-5,y+3);
        path.lineTo(x-5,y-3);
        path.close();
        paint.setColor(Color.RED);
        canvas.drawPath(path,paint);

        Log.i("TAG","x="+x+",y="+y);


        ImageView view = Control_getPic.getImageView();
        view.setImageBitmap(bmp);

    }

    /**
     * 导航模式下 画当前点 画目标点
     * @param lx 当前点x
     * @param ly 当前点y
     * @param angle 当前点旋转角度
     * @param dx 目标点x
     * @param dy 目标点y
     * @param bitmap 地图
     */
    public void draw_point(float lx,float ly,float angle,float dx,float dy,float dAngle,Bitmap bitmap){
        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();

        int x,y;
        x=(int)(lx*100/5);
        y=picH-(int)(ly*100/5);


        if(Navigation.getDockSites()!=null) {
            //任务点
            paint.setColor(Color.GREEN);
            for (DockSite dockSite : Navigation.getDockSites().values()) {
                float dock_x = dockSite.x;
                float dock_y = picH - dockSite.y;
                float dock_angle = dockSite.angle;

                canvas.rotate(-dock_angle, dock_x, dock_y);
                Path path = new Path();
                path.moveTo(dock_x + 5, dock_y);
                path.lineTo(dock_x - 5, dock_y + 3);
                path.lineTo(dock_x - 5, dock_y - 3);
                path.close();
                canvas.drawPath(path, paint);
                canvas.rotate(dock_angle, dock_x, dock_y);
//            canvas.drawCircle(dock_x,dock_y,3,paint);
                canvas.drawText(dockSite.name, dock_x - 1, dock_y - 4, paint);
            }
        }

        //目标点
        dy=picH-dy;
        paint.setColor(Color.RED);
        canvas.rotate(-dAngle,dx,dy);
        Path path1=new Path();
        path1.moveTo(dx+5,dy);
        path1.lineTo(dx-5,dy+3);
        path1.lineTo(dx-5,dy-3);
        path1.close();
        canvas.drawPath(path1,paint);
        canvas.rotate(dAngle,dx,dy);

//        canvas.drawCircle(dx,picH-dy,3,paint);


        //当前点
        canvas.rotate(-angle,x,y);
        Path path=new Path();
        path.moveTo(x+5,y);
        path.lineTo(x-5,y+3);
        path.lineTo(x-5,y-3);
        path.close();
        paint.setColor(Color.BLUE);
        canvas.drawPath(path,paint);



        ImageView view = Navigation.getImageView();
        view.setImageBitmap(bmp);
    }


    public void StopMove(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    byte stop[] = new byte[5];
                    stop[0] = stopCharge.getHead() ;
                    stop[1] = stopCharge.getCmd();
                    stop[2] = stopCharge.getLength();
                    stop[3] = stopCharge.getData();
                    stop[4] = stopCharge.getBack();
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(stop);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void recordCharge(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    byte send[]=new byte[5];
                    send[0]=recordCharge.getHead();
                    send[1]=recordCharge.getCmd();
                    send[2]=recordCharge.getLength();
                    send[3]=recordCharge.getData();
                    send[4]=recordCharge.getBack();
                    OutputStream outputStream=socket.getOutputStream();
                    outputStream.write(send);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void sendStereo(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] send=new byte[5];
                    send[0]=(byte)0xaa;
                    send[1]=(byte)0x01;
                    send[2]=1;
                    send[3]=(byte)0x03;
                    send[4]=(byte)0x77;
                    OutputStream outputStream=socket.getOutputStream();
                    outputStream.write(send);
                    Log.i("tag","stereo发送成功");
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void sendStereoIMU(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] send=new byte[5];
                    send[0]=(byte)0xaa;
                    send[1]=(byte)0x01;
                    send[2]=1;
                    send[3]=(byte)0x04;
                    send[4]=(byte)0x77;
                    OutputStream outputStream=socket.getOutputStream();
                    outputStream.write(send);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void sendSaveStereoMap(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] send=new byte[5];
                    send[0]=(byte)0xaa;
                    send[1]=(byte)0x05;
                    send[2]=1;
                    send[3]=(byte)0x05;
                    send[4]=(byte)0x77;
                    OutputStream outputStream=socket.getOutputStream();
                    outputStream.write(send);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public boolean isRunFlag() {
        return runFlag;
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
                MainActivity.getEdPort().setEnabled(!isConnected);  //端口
                MainActivity.getEdIp().setEnabled(!isConnected);
                MainActivity.getBnConnect().setText(isConnected?"断开":"连接");
            }
        });
    }
    public static int byteArrayToInt(byte[] b) {
        return   b[0] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16 |
                (b[3] & 0xFF) << 24;
    }

    public static byte[] intToByte(int val){
        byte[] b = new byte[4];
        b[0] = (byte)(val & 0xff);
        b[1] = (byte)((val >> 8) & 0xff);
        b[2] = (byte)((val >> 16) & 0xff);
        b[3] = (byte)((val >> 24) & 0xff);
        return b;
    }


    public static float byteToFloat(byte[] b) {
        // 4 bytes
        int accum = 0;
        for ( int shiftBy = 0; shiftBy < 4; shiftBy++ ) {
            accum |= (b[shiftBy] & 0xff) << shiftBy * 8;
        }
        return Float.intBitsToFloat(accum);
    }
    public static void FloatToByte(byte[] bb, float x) {
        // byte[] b = new byte[4];
        int l = Float.floatToIntBits(x);
        for (int i = 0; i < 4; i++) {
            bb[i] = new Integer(l).byteValue();
            l = l >> 8;
        }
    }
    public int getPicH() {
        return picH;
    }

    public int getPicW() {
        return picW;
    }

    public float getOriginal_pointX() {
        return original_pointX;
    }

    public float getOriginal_pointY() {
        return original_pointY;
    }

    public float getOriginal_pointAngle() {
        return original_pointAngle;
    }

    public float getCharge_pointX() {
        return charge_pointX;
    }

    public float getCharge_pointY() {
        return charge_pointY;
    }

    public float getCharge_pointAngle() {
        return charge_pointAngle;
    }

    public void isFinish_recevPic(boolean finish_recevPic) {
        this.finish_recevPic = finish_recevPic;
    }

    public boolean getSave_map_success() {
        return saveMapSuccessFlag;
    }

    public void setMybitmap(Bitmap mybitmap) {
        this.mybitmap = mybitmap;
    }

    public Bitmap getMybitmap() {
        return mybitmap;
    }

    public byte[] getEditBuff() {
        return editBuff;
    }

    public void setPicW(int picW) {
        this.picW = picW;
    }

    public void setPicH(int picH) {
        this.picH = picH;
    }

    public boolean isAchieveGoalFlag() {
        return achieveGoalFlag;
    }

    public void setAchieveGoalFlag(boolean achieveGoalFlag) {
        this.achieveGoalFlag = achieveGoalFlag;
    }
    public void clearStereoPoints(){
        this.stereoPoints.clear();
        stereoWidth=0;
        stereoHeight=0;
        maxStereoWidth=0;
        maxStereoHeight=0;
    }
}
