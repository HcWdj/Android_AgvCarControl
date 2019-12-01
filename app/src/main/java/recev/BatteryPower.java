package recev;

public class BatteryPower {
    private byte head;    //帧头
    private byte cmd;     //辨识符
    private byte length;   //长度
    private int status;
    private float voltage;
    private byte back;    //帧尾

    private static BatteryPower mBatteryPower=null;
    public static BatteryPower getInstance_batteryPower(){
        if(mBatteryPower==null){
            synchronized (BatteryPower.class){
                if(mBatteryPower==null)
                    mBatteryPower=new BatteryPower();
            }
        }
        return mBatteryPower;
    }
    public BatteryPower(){
        this.head=(byte)0xaa;
        this.cmd=(byte)0x83;
        this.length=8;
        this.back=0x77;
    }

    public void set(int status,float voltage){
        this.status=status;
        this.voltage=voltage;
    }

    public int getStatus() {
        return status;
    }

    public float getVoltage() {
        return voltage;
    }

    public byte getCmd() {
        return cmd;
    }

    public byte getLength() {
        return length;
    }
}
