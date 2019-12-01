package MyClass;

public class TaskChainItem {
    private String itemName;
    private int delay;
    public TaskChainItem(String itemName,int delay){
        this.itemName=itemName;
        this.delay=delay;
    }

    public String getItemName() {
        return itemName;
    }

    public int getDelay() {
        return delay;
    }
}
