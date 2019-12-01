package recev;

public class Map_xy {
    private byte head;    //帧头
    private byte cmd;     //辨识符
    private byte length;   //长度
    private float x=0,y=0,angle=0;    //坐标
    private byte back;    //帧尾

    private static Map_xy mMap_xy=null;
    public static Map_xy getInstance_map_xy(){
        if(mMap_xy==null){
            synchronized (Map_xy.class){
                if(mMap_xy==null){
                    mMap_xy=new Map_xy();
                }
            }
        }
        return mMap_xy;
    }

    public Map_xy(){
        this.head=(byte) 0xAA;
        this.cmd =(byte) 0x80;
        this.length=12;
        this.back=(byte) 0x77;
    }

    public byte getHead() {
        return head;
    }

    public byte getCmd() {
        return cmd;
    }

    public byte getLength() {
        return length;
    }

    public byte getBack() {
        return back;
    }

    public void set(float x,float y,float angle){
        this.x=x;
        this.y=y;
        this.angle=angle;
    }

    public float getY() {
        return y;
    }

    public float getX() {
        return x;
    }

    public float getAngle() {
        return angle;
    }
}
