package recev;

public class GoalAchieve {
    private byte head;    //帧头
    private byte cmd;     //辨识符
    private byte length;   //长度
    private byte data;
    private byte back;    //帧尾

    private static GoalAchieve mGoalAchieve=null;
    public static GoalAchieve getInstance_goal_achieve(){
        if(mGoalAchieve==null){
            synchronized (GoalAchieve.class){
                if(mGoalAchieve==null)
                    mGoalAchieve=new GoalAchieve();
            }
        }
        return mGoalAchieve;
    }

    public GoalAchieve(){
        this.head=(byte)0xaa;
        this.cmd=(byte)0x82;
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

    public byte getData() {
        return data;
    }

    public byte getBack() {
        return back;
    }
}
