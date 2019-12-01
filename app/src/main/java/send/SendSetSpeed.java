package send;

public class SendSetSpeed {
    private byte head;    //帧头
    private byte cmd;     //辨识符
    private byte length;   //长度
    private float lineSpeed=0,angleSpeed=0;    //坐标
    private byte back;    //帧尾

    private static SendSetSpeed mSendSetSpeed=null;
    public static SendSetSpeed getInstance_SendSetSpeed(){
        if(mSendSetSpeed==null){
            synchronized (TargetPoint.class){
                if(mSendSetSpeed==null){
                    mSendSetSpeed=new SendSetSpeed();
                }
            }
        }
        return mSendSetSpeed;
    }

    private SendSetSpeed(){
        this.head=(byte) 0xAA;
        this.cmd=(byte) 0x03;
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

    public float getLineSpeed() {
        return lineSpeed;
    }

    public float getAngleSpeed() {
        return angleSpeed;
    }

    public void setLineSpeed(float lineSpeed) {
        this.lineSpeed = lineSpeed;
    }

    public void setAngleSpeed(float angleSpeed) {
        this.angleSpeed = angleSpeed;
    }
}
