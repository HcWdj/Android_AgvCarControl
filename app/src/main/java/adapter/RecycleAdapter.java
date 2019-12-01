package adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.along.agvcontrol0412.R;

import java.util.List;

import MyClass.TaskChainItem;

public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.MyViewHolder>{
    private Context context;
    private List<TaskChainItem> list;
    private View inflater;

    public RecycleAdapter(Context context,List<TaskChainItem>list){
        this.context=context;
        this.list=list;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        inflater=LayoutInflater.from(context).inflate(R.layout.item,parent,false);
        MyViewHolder myViewHolder=new MyViewHolder(inflater);
        return myViewHolder;
    }

    public void onBindViewHolder(MyViewHolder holder, int position){
        holder.textView.setText(
                "              任务"
                +(position+1)
                +".  "
                +list.get(position).getItemName()
                +"          "
                +"延时"
                +list.get(position).getDelay()
                +"秒"
        );
    }

    public int getItemCount(){
        return list.size();
    }



    class MyViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        public MyViewHolder(View itemView){
            super(itemView);
            textView=itemView.findViewById(R.id.dialog_textView);
        }
    }


}
