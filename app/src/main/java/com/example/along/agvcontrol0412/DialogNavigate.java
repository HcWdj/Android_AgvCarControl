package com.example.along.agvcontrol0412;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import MyClass.DockSite;
import MyClass.TaskChainItem;
import adapter.RecycleAdapter;


public class DialogNavigate extends AppCompatActivity implements View.OnClickListener{
    private Button btAddTask,btDelete,btRelease;
    private RecyclerView recyclerView;
    private RecycleAdapter adapter;
    private Context context;
    private ArrayList<TaskChainItem> list=new ArrayList<>();
    private HashMap<String,DockSite>dockSites=new HashMap<>();
    private Handler handler =new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_navigate);
        dockSites=Navigation.getDockSites();
        context=this;

        btAddTask=this.findViewById(R.id.addTask);
        btDelete=this.findViewById(R.id.deleteTask);
        btRelease=this.findViewById(R.id.ReleaseTask);
        recyclerView=this.findViewById(R.id.recycleView);
        btAddTask.setOnClickListener(this);
        btDelete.setOnClickListener(this);
        btRelease.setOnClickListener(this);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.addTask:
                addTask();
                break;
            case R.id.deleteTask:
                deleteTask();
                break;
            case R.id.ReleaseTask:
                releaseTask();
                break;
        }
    }

    private void releaseTask(){
        Navigation.performTaskChain(list);
    }


    private void deleteTask(){
        final EditText editText=new EditText(this);
        AlertDialog.Builder radioDialog=new AlertDialog.Builder(this);
        radioDialog.setTitle("删除任务序号：")
                .setView(editText)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int num=Integer.parseInt(editText.getText().toString())-1;
                        if(num<0 || num>=list.size()){
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DialogNavigate.this, "输入有误", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else{
                            list.remove(num);
                            displayRecycleView();
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }


    private void addTask(){
        final EditText editText=new EditText(this);
        final int[] chooseItem = new int[1];
        final String radioItems[]=new String[dockSites.size()];
        Set<String> s_keys=dockSites.keySet();
        List<String> l_keys=new ArrayList<>(s_keys);
        for(int i=0;i<dockSites.size();i++)
            radioItems[i]=l_keys.get(i);
        AlertDialog.Builder radioDialog=new AlertDialog.Builder(this);
        radioDialog.setTitle("任务")
                .setView(editText)
                .setSingleChoiceItems(radioItems, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseItem[0] =which;
                    }
                })
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String str=editText.getText().toString();
                        str=str.replaceAll("[0-9]","");
                        if(str.length()==0 && !editText.getText().toString().equals("")) {
                            list.add(new TaskChainItem(dockSites.get(radioItems[chooseItem[0]]).name, Integer.parseInt(editText.getText().toString())));
                            displayRecycleView();
                        }
                        else{
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DialogNavigate.this, "输入有误", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();

    }

    private void displayRecycleView(){
        adapter=new RecycleAdapter(context,list);
        LinearLayoutManager manager=new LinearLayoutManager(context);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
    }


}
