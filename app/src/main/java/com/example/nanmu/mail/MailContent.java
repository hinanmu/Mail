package com.example.nanmu.mail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.mail.MessagingException;

public class MailContent extends AppCompatActivity {
    private TextView tv_re_personal_add;
    private TextView tv_re_personal_name;
    private TextView tv_re_recipient_add;
    private TextView tv_re_recipient_name;
    private TextView tv_re_subject;
    private TextView tv_re_date;
    private TextView tv_re_plain_content;
    private WebView wv_re_html_content;
    private SharedPreferences mySharedPreferences;
    private String userName;
    private String uid;
    private  ParseEml parseEml;
    private Toolbar toolbar;
    private List fileNames;
    private FileRecyclerAdapter myRecyclerAdapter;
    private RecyclerView recyclerView;
    private String folder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mail_content);

        init();
        try {
            analysisEml();
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadInfo();

    }

    public void  init()
    {
        tv_re_date = (TextView) findViewById(R.id.tv_re_date);
        tv_re_subject = (TextView) findViewById(R.id.tv_re_subject);
        tv_re_personal_add = (TextView) findViewById(R.id.tv_re_personal_add);
        tv_re_personal_name = (TextView) findViewById(R.id.tv_re_personal_name);
        tv_re_recipient_add = (TextView) findViewById(R.id.tv_re_recipient_add);
        tv_re_recipient_name=(TextView) findViewById(R.id.tv_re_recipient_name);
        tv_re_plain_content = (TextView) findViewById(R.id.tv_re_plain_content);
        wv_re_html_content = (WebView) findViewById(R.id.wv_re_html_content);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        //获得上个页面传来的数据
        Intent intent = getIntent();
        folder = intent.getStringExtra("folder");
        uid = intent.getStringExtra("uid");
        System.out.println("得到信件uid，准备显示该信件"+uid);
        //得到用户名
        mySharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        userName = mySharedPreferences.getString("userName","");

        toolbar.setTitle("读邮件");
        setSupportActionBar(toolbar);
        //设置了回退按钮
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //得到信件uid准备解析

    }


    public void analysisEml() throws Exception {
        String filePath = "/sdcard/SoftMail/"+userName+"/"+folder+"/";
        parseEml = new ParseEml(userName);
        parseEml.parserFile(filePath+uid+".eml");
        parseEml.parseEmlFile();
    }

    public void loadInfo()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        tv_re_date.setText(sdf.format(parseEml.getDate()));
        tv_re_subject.setText("主题:"+parseEml.getMailSubject());
        tv_re_personal_add.setText(parseEml.getPersonalAdd());
        tv_re_personal_name.setText(parseEml.getPersonalName());
        tv_re_recipient_add.setText(parseEml.getRecipientAdd());
        tv_re_recipient_name.setText(parseEml.getRecipientName());
        //tv_re_plain_content.setText(parseEml.getPlainContent());

        wv_re_html_content.post(new Runnable() {
            @Override
            public void run() {
                wv_re_html_content.loadData(parseEml.getHtmlContent(), "text/html; charset=UTF-8",null);
            }
        });
//添加回复按钮点击
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()){
                    case R.id.action_reply:
                        Intent intent = new Intent(MailContent.this,SendMail.class);
                        intent.putExtra("personalAdd",parseEml.getPersonalAdd());
                        startActivity(intent);
                        break;
                    case R.id.action_delete:
                        try {
                            delete();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (MessagingException e) {
                            e.printStackTrace();
                        }
                       intent = new Intent(MailContent.this,MainActivity.class);
                       startActivity(intent);
                }
                return true;
            }
        });


        fileNames = parseEml.getFileName();
        myRecyclerAdapter=new FileRecyclerAdapter(this,fileNames);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view_re_file);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //设置布局管理器
        recyclerView.setLayoutManager(layoutManager);
        //设置为垂直布局，这也是默认的
        layoutManager.setOrientation(OrientationHelper.VERTICAL);
        //设置Adapter
        recyclerView.setAdapter(myRecyclerAdapter);
        //设置增加或删除条目的动画
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        myRecyclerAdapter.setOnMailItemClickListener(new FileRecyclerAdapter.OnMailItemClickListener() {
            @Override
            public void onItemClick(int position) {
                File file = new File("/sdcard/SoftMail/"+userName+"/doc/"+fileNames.get(position));
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //设置intent的Action属性
                intent.setAction(Intent.ACTION_VIEW);
                //获取文件file的MIME类型
                String type = getMIMEType(file);
                //设置intent的data和Type属性。
                intent.setDataAndType(/*uri*/Uri.fromFile(file), type);
                //跳转
                startActivity(intent);
            }
        });

    }

    public void delete() throws IOException, MessagingException {
        System.out.println("准备删除文件");
        if(folder.equals("inbox") || folder.equals("send"))
        {
            parseEml.getMsg().writeTo(new FileOutputStream("/sdcard/SoftMail/"+userName+"/delete/"+uid+".eml"));
        }
        System.out.println("准备删除文件");
        String filePath = "/sdcard/SoftMail/"+userName+"/"+folder+"/"+uid+".eml";
        System.out.println("准备删除文件"+filePath);
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            file.delete();
        }
        System.out.println("删除完成");
        Toast.makeText(getApplicationContext(), "删除成功" ,
                Toast.LENGTH_LONG).show();
    }

    //设置右上角界面
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mail_content_toolbar, menu);
        return true;
    }

    private String getMIMEType(File file) {

        String type="*/*";
        String fName = file.getName();
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if(dotIndex < 0){
            return type;
        }
    /* 获取文件的后缀名 */
        String end=fName.substring(dotIndex,fName.length()).toLowerCase();
        if(end=="")return type;
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for(int i=0;i<MIME_MapTable.length;i++){ //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if(end.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    private final String[][] MIME_MapTable={
            //{后缀名， MIME类型}
            {".3gp",    "video/3gpp"},
            {".apk",    "application/vnd.android.package-archive"},
            {".asf",    "video/x-ms-asf"},
            {".avi",    "video/x-msvideo"},
            {".bin",    "application/octet-stream"},
            {".bmp",    "image/bmp"},
            {".c",  "text/plain"},
            {".class",  "application/octet-stream"},
            {".conf",   "text/plain"},
            {".cpp",    "text/plain"},
            {".doc",    "application/msword"},
            {".docx",   "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".xls",    "application/vnd.ms-excel"},
            {".xlsx",   "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".exe",    "application/octet-stream"},
            {".gif",    "image/gif"},
            {".gtar",   "application/x-gtar"},
            {".gz", "application/x-gzip"},
            {".h",  "text/plain"},
            {".htm",    "text/html"},
            {".html",   "text/html"},
            {".jar",    "application/java-archive"},
            {".java",   "text/plain"},
            {".jpeg",   "image/jpeg"},
            {".jpg",    "image/jpeg"},
            {".js", "application/x-javascript"},
            {".log",    "text/plain"},
            {".m3u",    "audio/x-mpegurl"},
            {".m4a",    "audio/mp4a-latm"},
            {".m4b",    "audio/mp4a-latm"},
            {".m4p",    "audio/mp4a-latm"},
            {".m4u",    "video/vnd.mpegurl"},
            {".m4v",    "video/x-m4v"},
            {".mov",    "video/quicktime"},
            {".mp2",    "audio/x-mpeg"},
            {".mp3",    "audio/x-mpeg"},
            {".mp4",    "video/mp4"},
            {".mpc",    "application/vnd.mpohun.certificate"},
            {".mpe",    "video/mpeg"},
            {".mpeg",   "video/mpeg"},
            {".mpg",    "video/mpeg"},
            {".mpg4",   "video/mp4"},
            {".mpga",   "audio/mpeg"},
            {".msg",    "application/vnd.ms-outlook"},
            {".ogg",    "audio/ogg"},
            {".pdf",    "application/pdf"},
            {".png",    "image/png"},
            {".pps",    "application/vnd.ms-powerpoint"},
            {".ppt",    "application/vnd.ms-powerpoint"},
            {".pptx",   "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {".prop",   "text/plain"},
            {".rc", "text/plain"},
            {".rmvb",   "audio/x-pn-realaudio"},
            {".rtf",    "application/rtf"},
            {".sh", "text/plain"},
            {".tar",    "application/x-tar"},
            {".tgz",    "application/x-compressed"},
            {".txt",    "text/plain"},
            {".wav",    "audio/x-wav"},
            {".wma",    "audio/x-ms-wma"},
            {".wmv",    "audio/x-ms-wmv"},
            {".wps",    "application/vnd.ms-works"},
            {".xml",    "text/plain"},
            {".z",  "application/x-compress"},
            {".zip",    "application/x-zip-compressed"},
            {"",        "*/*"}
    };

}
