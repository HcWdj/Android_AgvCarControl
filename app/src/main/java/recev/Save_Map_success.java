package recev;

public class Save_Map_success {
    private byte head;    //帧头
    private byte cmd;     //辨识符
    private byte length;   //长度
    private byte data;
    private byte back;    //帧尾

    private static Save_Map_success msave_map_success=null;
    public static Save_Map_success getInstance(){
        if(msave_map_success==null){
            synchronized(Save_Map_success.class){
                if(msave_map_success==null){
                    msave_map_success=new Save_Map_success();
                }
            }
        }
        return msave_map_success;
    }
    public Save_Map_success(){
        this.head=(byte)0xaa;
        this.cmd=(byte)0x81;
        this.length=1;
        this.data=1;
        this.back=0x77;
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

    public byte getData() {
        return data;
    }
}
