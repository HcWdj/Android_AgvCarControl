package send;


public class Map_xy_send {
    private byte head;    //帧头
    private byte cmd;     //辨识符
    private byte length;   //长度
    private float x,y,angle;    //坐标   改成float data[2] data[0]=x,data[1]=y
    private byte back;    //帧尾

    private static Map_xy_send mMap_xy_send=null;
    public static Map_xy_send getInstance_map_xy_send(){
        if(mMap_xy_send==null){
            synchronized (Map_xy_send.class){
                if(mMap_xy_send==null){
                    mMap_xy_send=new Map_xy_send();
                }
            }
        }
        return mMap_xy_send;
    }

    private Map_xy_send(){
        this.head= (byte) 0xAA;
        this.cmd =(byte) 0x04;
        this.length=12;//3个4byte
        this.back=(byte) 0x77;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public byte getLength() {
        return length;
    }

    public byte getHead() {
        return head;
    }

    public byte getCmd() {
        return cmd;
    }

    public byte getBack() {
        return back;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getAngle() {
        return angle;
    }
}
