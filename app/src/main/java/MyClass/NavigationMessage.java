package MyClass;

import android.graphics.Bitmap;

import java.util.HashMap;

public class NavigationMessage {

    private int mode=0;// 0:无   1:navigate
    private String imagePath;
    private Bitmap bitmap;
    private HashMap<String,DockSite> dockSites=new HashMap<>();

    private static NavigationMessage mNavigationMessage=null;
    public static NavigationMessage getInstance_navigationMessage(){
        if(mNavigationMessage==null){
            synchronized (MyPicture.class){
                if(mNavigationMessage==null){
                    mNavigationMessage=new NavigationMessage();
                }
            }
        }
        return mNavigationMessage;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public void setDockSites(HashMap<String, DockSite> dockSites) {
        this.dockSites = dockSites;
    }

    public int getMode() {
        return mode;
    }

    public String getImagePath() {
        return imagePath;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public HashMap<String, DockSite> getDockSites() {
        return dockSites;
    }

    public void clearMessage(){
        imagePath=null;
        bitmap=null;
        dockSites.clear();
    }

}
