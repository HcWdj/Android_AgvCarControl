package send;

public class NavigationMap {
    private byte head;    //帧头
    private byte cmd;     //辨识符
    private int length;   //发送的数据长度
    private int row,column;    //地图的行数、列数
    private float origin_x,origin_y;
    private byte[] data;     //地图数据
    private int statesNum;
    private byte back;    //帧尾

    private static NavigationMap mNavigationMap=null;
    public static NavigationMap getInstance_navigationMap(){
        if(mNavigationMap==null){
            synchronized (NavigationMap.class){
                if(mNavigationMap==null){
                    mNavigationMap=new NavigationMap();
                }
            }
        }
        return mNavigationMap;
    }

    private NavigationMap(){
        this.head= (byte) 0xAA;
        this.cmd =(byte) 0x40;

        this.back=(byte) 0x77;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setStatesNum(int statesNum) {
        this.statesNum = statesNum;
    }

    public void setOrigin_x(float origin_x) {
        this.origin_x = origin_x;
    }

    public void setOrigin_y(float origin_y) {
        this.origin_y = origin_y;
    }

    public byte getHead() {
        return head;
    }

    public byte getCmd() {
        return cmd;
    }

    public int getLength() {
        return length;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public byte getBack() {
        return back;
    }

    public byte[] getData() {
        return data;
    }

    public int getStatesNum() {
        return statesNum;
    }

    public float getOrigin_x() {
        return origin_x;
    }

    public float getOrigin_y() {
        return origin_y;
    }
}
