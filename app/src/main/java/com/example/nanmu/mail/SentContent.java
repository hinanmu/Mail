package com.example.nanmu.mail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SentContent extends Fragment {

    private RecyclerView recyclerView;
    private InboxRecyclerAdapter myRecyclerAdapter;
    private List<ParseEml> parseEmls;
    private SharedPreferences mySharedPreferences;
    private String userName ;
    private SwipeRefreshLayout refresh_layout_send;
    private MyHandler myHandler;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        System.out.println("开始刷新sendcontent");
        View view = inflater.inflate(R.layout.fragment_sent_content, container, false);
        init();
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_send);

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_send);
        refresh_layout_send = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout_send);
//        refresh_layout_send.setProgressViewOffset(false, 0, (int) TypedValue
//                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources()
//                        .getDisplayMetrics()));
//        refresh_layout_send.setRefreshing(true);
        //第一次进入出现刷新动画
        System.out.println("开始初次刷新sendContent");

        new Thread(){
            @Override
            public void run() {
                try {
                    ananylsisEml();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        refresh_layout_send.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {//下拉刷新
            @Override
            public void onRefresh() {
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            ananylsisEml();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity().getBaseContext());
        //设置布局管理器
        recyclerView.setLayoutManager(layoutManager);
        //设置为垂直布局，这也是默认的
        layoutManager.setOrientation(OrientationHelper.VERTICAL);
        //设置Adapter
        //设置增加或删除条目的动画
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        return view;
    }

    public void init()
    {
        mySharedPreferences = getActivity().getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        userName = mySharedPreferences.getString("userName", "");
        myHandler = new MyHandler();
        parseEmls=new ArrayList<>();

    }

    public void refresh() throws Exception {

        Collections.sort(parseEmls);
        myRecyclerAdapter=new InboxRecyclerAdapter(getActivity().getBaseContext(),parseEmls);
        myRecyclerAdapter.setOnMailItemClickListener(new InboxRecyclerAdapter.OnMailItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(getActivity(),MailContent.class);
                intent.putExtra("folder", "send");
                intent.putExtra("uid", parseEmls.get(position).getUid());
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(myRecyclerAdapter);

    }

    public void ananylsisEml() throws Exception {

        System.out.println("开始分析sendContent");

        String filePath = "/sdcard/SoftMail/"+userName+"/send/";
        List<String> inboxFileLis = getInboxFileLis();
        ParseEml parseEml;

        System.out.println("发件箱文件数量"+inboxFileLis.size());
        for (int i = 0;i < inboxFileLis.size();i ++) {
            boolean isDisplay = false;

            for(int j = 0;j < parseEmls.size();j++)
            {
                if(inboxFileLis.get(i).equals(parseEmls.get(j).getUid()+".eml"))
                {
                    isDisplay = true;
                }
            }
            if (isDisplay == false)
            {
                System.out.println("开始分析文件"+inboxFileLis.get(i));
                parseEml = new ParseEml(userName);
                parseEml.parserFile(filePath+inboxFileLis.get(i));
                parseEmls.add(parseEml);
            }
        }
        System.out.println("发送发件箱处理完成消息");
        sendMsg("true");

    }
    public List<String> getInboxFileLis()
    {
        List fileNameLis = new ArrayList();
        File file = new File("/sdcard/SoftMail/"+userName+"/send/");
        File[] subFile = file.listFiles();
        for (int iFileLength = 0; iFileLength < subFile.length; iFileLength++) {
            // 判断是否为文件夹
            if (!subFile[iFileLength].isDirectory()) {
                String filename = subFile[iFileLength].getName();
                // 判断是否为eml结尾
                if (filename.trim().toLowerCase().endsWith(".eml")) {
                    fileNameLis.add(filename);
                }
            }
        }
        return fileNameLis;
    }

    public void sendMsg(String str)
    {
        Message msg = new Message();
        Bundle b = new Bundle();// 存放数据
        b.putString("connect", str);
        msg.setData(b);

        myHandler.sendMessage(msg);
    }


    class MyHandler extends Handler {
        public MyHandler() {
        }

        public MyHandler(Looper L) {
            super(L);
        }

        // 子类必须重写此方法，接受数据
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            // 此处可以更新UI
            Bundle b = msg.getData();
            String connect = b.getString("connect");
            if(connect.equals("true"))
            {
                System.out.println("发件箱刷新完成");

                try {
                    refresh();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Toast.makeText(getActivity(), "刷新完成" ,
                        Toast.LENGTH_LONG).show();
                refresh_layout_send.setRefreshing(false);
                for(int i = 0; i < parseEmls.size();i++)
                {
                    System.out.println("发件箱已加载"+parseEmls.size());
                    System.out.println(parseEmls.get(i).getUid());
                }
            }
            else {

            }

        }
    }
}
