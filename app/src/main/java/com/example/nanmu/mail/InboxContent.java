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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.apache.commons.mail.util.MimeMessageParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class InboxContent extends Fragment {

    private RecyclerView recyclerView;
    private InboxRecyclerAdapter myRecyclerAdapter;
    private List<ParseEml> parseEmls;
    private  SharedPreferences mySharedPreferences;
    private int inboxNum;
    private String pop3Server;
    private String userName ;
    private String userPass;
    private POP3Demo pop3Demo;
    private FloatingActionButton btn_send_mail;
    private MyHandler myHandler;
    private SwipeRefreshLayout refresh_layout;
    private int lastVisibleItem;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mySharedPreferences = getActivity().getSharedPreferences("userInfo",Context.MODE_PRIVATE);
        pop3Server = "pop." + mySharedPreferences.getString("userName", "").split("@")[1];
        userName = mySharedPreferences.getString("userName", "");
        userPass = mySharedPreferences.getString("userPass", "");
        parseEmls=new ArrayList<>();
        pop3Demo = new POP3Demo(pop3Server, userName, userPass);


        View view = inflater.inflate(R.layout.fragment_inbox_content, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_inbox);
        refresh_layout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        refresh_layout.post(new Runnable() {
            @Override
            public void run() {
                refresh_layout.setRefreshing(true);
            }
        });//第一次进入出现刷新动画
        new refreshMailList().start();//第一次刷新
        refresh_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {//下拉刷新
            @Override
            public void onRefresh() {
                new refreshMailList().start();
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

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(newState == RecyclerView.SCROLL_STATE_DRAGGING && lastVisibleItem + 1 == myRecyclerAdapter.getItemCount())
                {
                    System.out.println("执行加载动画");
                    refresh_layout.setRefreshing(true);
                    new Thread(){
                        @Override
                        public void run() {
                            try {
                                System.out.println("执行加载函数");
                                loadMore();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                lastVisibleItem = layoutManager.findLastVisibleItemPosition();
            }

        });


        myHandler = new MyHandler();

        btn_send_mail = (FloatingActionButton) view.findViewById(R.id.btn_send_mail);
        btn_send_mail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(),SendMail.class);
                startActivity(intent);
            }
        });
        return view;
    }


    public class refreshMailList extends Thread{
        public void run() {
            try {
                initLoad();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void initAdapter() throws Exception {
        ananylsisEml();
        Collections.sort(parseEmls);
        myRecyclerAdapter=new InboxRecyclerAdapter(getActivity().getBaseContext(),parseEmls);
        myRecyclerAdapter.setOnMailItemClickListener(new InboxRecyclerAdapter.OnMailItemClickListener() {
            @Override
            public void onItemClick(int position) {
               Intent intent = new Intent(getActivity(),MailContent.class);
                intent.putExtra("folder", "inbox");
                intent.putExtra("uid", parseEmls.get(position).getUid());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(myRecyclerAdapter);
    }

    public  void  loadMore() throws Exception {
        System.out.println("开始加载");
        String uidTemp = "";
        Integer numTemp = 0;
        pop3Demo.refresh();
        uidTemp = parseEmls.get(parseEmls.size()-1).getUid();
        numTemp = pop3Demo.getUidToNum().get(uidTemp);

        System.out.println("uiduiduiduiduid"+uidTemp);
        System.out.println("uiduiduiduiduid"+numTemp);
        for(int i = numTemp -1; i > ((numTemp - 1)>5 ? numTemp - 1 - 5:0);i-- )
        {
            System.out.println("加载加载iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii"+i);
            pop3Demo.downloadMail(i);
            System.out.println("下载完成iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii"+i);
        }
        System.out.println("开始分析");
        sendMsg("true");
    }


    public void initLoad() throws IOException {
        List<String> inboxFileLis = getInboxFileLis();
        String uidNameFile = "";
        if (pop3Demo.connect()) {
            System.out.println("开始下载");
            pop3Demo.refresh();
            inboxNum = pop3Demo.getInboxNum();

            if (inboxFileLis.isEmpty())
            {
                for (int i = 1; i <= (inboxNum > 10 ? 10 : inboxNum); i++) {
                    int num= inboxNum - i + 1;
                    uidNameFile = pop3Demo.getNumToUid().get(num)+".eml";
                    if (!inboxFileLis.contains(uidNameFile)) {
                        pop3Demo.downloadMail(num);
                    }
                }
            }
            else
            {
                for (int i = 1; i <= inboxNum; i++)
                {
                    int num= inboxNum - i + 1;
                    uidNameFile = pop3Demo.getNumToUid().get(num)+".eml";
                    System.out.println("uiduiduiduiduidToTOTOTOOTO"+pop3Demo.getUidToNum().get(pop3Demo.getNumToUid().get(num)));
                    System.out.println("numnumnum"+num);
                    System.out.println("filefile"+uidNameFile);
                    System.out.println("aaaaaaaaaaaaaaaaaaaaaaaa"+uidNameFile);

                    if (!inboxFileLis.contains(uidNameFile)) {
                        System.out.println("未包含开始下载");
                        pop3Demo.downloadMail(num);
                        System.out.println("");
                    }
                    else
                    {
                        System.out.println("已加载到最新，退出");
                        break;
                    }
                }
            }
            sendMsg("true");
        }
        else
        {
            sendMsg("false");
        }

    }
    public void ananylsisEml() throws Exception {
        String filePath = "/sdcard/SoftMail/"+userName+"/inbox/";
        List<String> inboxFileLis = getInboxFileLis();
        ParseEml parseEml;

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
                parseEml = new ParseEml(userName);
                parseEml.parserFile(filePath+inboxFileLis.get(i));
                parseEmls.add(parseEml);
            }

        }

    }

    public List<String> getInboxFileLis()
    {
        List fileNameLis = new ArrayList();
        File file = new File("/sdcard/SoftMail/"+userName+"/inbox/");
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
                try {
                    initAdapter();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                refresh_layout.setRefreshing(false);
                Toast.makeText(getActivity(), "刷新完成" ,
                        Toast.LENGTH_LONG).show();

                for(int i = 0; i < parseEmls.size();i++)
                {
                    System.out.println("已加载"+parseEmls.size());
                    System.out.println(parseEmls.get(i).getUid());
                }
            }
            else {

            }

        }
    }
}
