package send;

public class TargetPoint {
    private byte head;    //帧头
    private byte cmd;     //辨识符
    private byte length;   //长度
    private float x=0,y=0;    //坐标
    private byte back;    //帧尾

    private static TargetPoint mTargetPoint=null;
    public static TargetPoint getInstance_TargetPoint(){
        if(mTargetPoint==null){
            synchronized (TargetPoint.class){
                if(mTargetPoint==null){
                    mTargetPoint=new TargetPoint();
                }
            }
        }
        return mTargetPoint;
    }

    private TargetPoint(){ //目前没用到
        this.head=(byte) 0xAA;
        this.cmd=(byte) 0x00;
        this.length=8;
        this.back=(byte)0x77;
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

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }
}
