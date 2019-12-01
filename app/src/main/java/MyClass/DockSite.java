package MyClass;

public class DockSite {
    public String name;
    public float x;
    public float y;
    public float angle;
    public DockSite(String name,float x,float y,float angle){
        this.name=name;
        this.x=x;
        this.y=y;
        this.angle=angle;
    }

    @Override
    public String toString() {
        return "name="+name
                +",x="+x
                +",y="+y
                +",angle="+angle;
    }
}
