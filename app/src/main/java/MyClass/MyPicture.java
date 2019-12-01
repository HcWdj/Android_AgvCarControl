package MyClass;

public class MyPicture {
    public int row=0;
    public int column=0;
    public int station_number=0;
    public float origin_x=0;
    public float origin_y=0;
    public float origin_angle=0;
    private static MyPicture myPicture=null;
    public static MyPicture getInstance_myPicture(){
        if(myPicture==null){
            synchronized (MyPicture.class){
                if(myPicture==null){
                    myPicture=new MyPicture();
                }
            }
        }
        return myPicture;
    }
    public MyPicture(){

    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setOrigin_x(float origin_x) {
        this.origin_x = origin_x;
    }

    public void setOrigin_y(float origin_y) {
        this.origin_y = origin_y;
    }

    public void setOrigin_angle(float origin_angle) {
        this.origin_angle = origin_angle;
    }

    public void setStation_number(int station_number) {
        this.station_number = station_number;
    }

    @Override
    public String toString() {
        return "row="+row+",column="+column+",station_number="+station_number;
    }
}
