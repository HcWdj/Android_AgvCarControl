package send;

public class AskForNavigation {
    private byte head;    //帧头
    private byte cmd;     //辨识符
    private byte length;   //长度
    private byte data;    //坐标
    private byte back;    //帧尾

    private static AskForNavigation mAskForNavigation=null;
    public static AskForNavigation getInstance_AskForNavigation(){
        if(mAskForNavigation==null){
            synchronized (AskForNavigation.class){
                if(mAskForNavigation==null){
                    mAskForNavigation=new AskForNavigation();
                }
            }
        }
        return mAskForNavigation;
    }

    private AskForNavigation(){
        this.head= (byte) 0xAA;
        this.cmd =(byte) 0x01;
        this.length=1;
        this.data=2;
        this.back=(byte) 0x77;
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

    public byte getData() {
        return data;
    }
}
