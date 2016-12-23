package com.example.nanmu.mail;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.io.FileOutputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


public class SendMail extends AppCompatActivity {

    private FloatingActionButton btn_add_file ;
    private EditText send_subject;
    private EditText send_content;
    private EditText send_add;
    private Toolbar toolbar; //
    private FileRecyclerAdapter myRecyclerAdapter;
    private RecyclerView recyclerView;
    private List<String> fileNames;
    private List<String> filePaths;
    private String userName;
    private String userPass;
    private SharedPreferences mySharedPreferences;
    private String sendPath;
    private String savePath;
    private MyHandler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_mail);

        Log.d("afaf","sendsendssssssssssssssssssssssssssssss");
        send_subject = (EditText) findViewById(R.id.send_subject);
        send_content = (EditText) findViewById(R.id.send_content);
        send_add = (EditText) findViewById(R.id.sent_add);
        myHandler = new MyHandler();
        //设置文件路径列表
        filePaths = new ArrayList<>();
        //设置文件列表
        fileNames = new ArrayList<>();

        //判断是否为回复信件
        Intent intent = getIntent();
        send_add.setText(intent.getStringExtra("personalAdd"));
        send_subject.setText(intent.getStringExtra("subject"));
        send_content.setText(intent.getStringExtra("content"));
//        filePaths = intent.getStringArrayListExtra("file");



        //得到用户名
        mySharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        userName = mySharedPreferences.getString("userName","");

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("写邮件");
        setSupportActionBar(toolbar);
        //设置了回退按钮
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()){
                    case R.id.action_save:
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    save();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        Log.d("草稿","fgaggagagagaggggggggggggggggggggggggggggggggggg");
                        break;
                    case R.id.action_send:
                        Toast.makeText(SendMail.this, "正在发送，请稍候" ,
                                Toast.LENGTH_LONG).show();
                        Log.d("afaf","ffffffffffffffffffffffffffffffffffff");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    send();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                }
                return true;
            }
        });

        btn_add_file = (FloatingActionButton) findViewById(R.id.btn_add_file);
        btn_add_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent,1);
            }
        });



        myRecyclerAdapter=new FileRecyclerAdapter(this,filePaths);
        myRecyclerAdapter.setOnMailItemClickListener(new FileRecyclerAdapter.OnMailItemClickListener() {
            @Override
            public void onItemClick(int position) {
                File file = new File(filePaths.get(position));
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
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view_file);
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


    }
    /**
     * 根据文件后缀名获得对应的MIME类型。
     * @param file
     */
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


    public void sendMsg(String type,String str )
    {
        android.os.Message msg = new android.os.Message();
        Bundle b = new Bundle();// 存放数据
        b.putString(type, str);

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
        public void handleMessage(android.os.Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            // 此处可以更新UI
            Bundle b = msg.getData();


            String send = b.getString("send");
            String save= b.getString("save");
            if(send != null && send.equals("true"))
            {
                try {
                    Toast.makeText(SendMail.this, "发送成功" ,
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(SendMail.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
            { if(send == null && save != null) {
                try {
                    Toast.makeText(SendMail.this, "发送失败",
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            }

            if(save != null && save.equals("true"))
            {
                try {
                    Toast.makeText(SendMail.this, "保存成功" ,
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(SendMail.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
            {
                if (save == null && send != null){
                    try {
                        Toast.makeText(SendMail.this, "保存失败" ,
                                Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                }
        }
    }
    //设置右上角界面
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_mail_toolbar, menu);
        return true;
    }

    public void save () throws Exception {
        mySharedPreferences = getSharedPreferences("userInfo",
                Context.MODE_PRIVATE);
        userName = mySharedPreferences.getString("userName", "");
        savePath = "/sdcard/SoftMail/" + userName + "/draft/";
        Session session = Session.getDefaultInstance(new Properties());

        if (isContainsChinese(send_add.getText().toString()))
        {
            send_add.setText("");
        }
        MimeMessage message = createMessage(session,send_add.getText().toString());
        Date date = new Date();
        message.writeTo(new FileOutputStream(savePath+date.toString()+".eml"));
        sendMsg("save","true");
    }

    public static boolean isContainsChinese(String str)
    {
        String regEx = "[\u4e00-\u9fa5]";
        Pattern pat = Pattern.compile(regEx);
        Matcher matcher = pat.matcher(str);
        boolean flg = false;
        if (matcher.find())    {
            flg = true;
        }
        return flg;
    }

    public void  send() throws Exception {

        mySharedPreferences = getSharedPreferences("userInfo",
                Context.MODE_PRIVATE);
        userName = mySharedPreferences.getString("userName", "");
        userPass = mySharedPreferences.getString("userPass","");
        sendPath = "/sdcard/SoftMail/"+userName+"/send/";

        for(int i = 0;i<send_add.getText().toString().split(";").length;i++)
        {
            String to = send_add.getText().toString().split(";")[i];
            if(to.contains("@"))
            {
                Session session = Session.getDefaultInstance(new Properties());
                MimeMessage message = createMessage(session,to);
                Date date = new Date();

                message.writeTo(new FileOutputStream(sendPath+date.toString()+".eml"));
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                message.writeTo(byteArrayOutputStream);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                byte[] buffer = new byte[1024];
                String res = "";
                int len = 0;
                while((len = byteArrayInputStream.read(buffer)) != -1)
                {
                    res += new String(buffer,0,len);
                }
                System.out.print(res);
                SMTPDemo smtpDemo = new SMTPDemo(userName,userPass,to,res);
                if (smtpDemo.connect())
                {
                    if(smtpDemo.sendMail())
                    {
                        sendMsg("send","true");
                    }
                    else
                    {
                        sendMsg("send","false");
                    }

                }
            }
            else
            {
                System.out.print("收信人错误收信人错误收信人错误收信人错误收信人错误");
            }
        }
    }
    /**
     * 根据传入的文件路径创建附件并返回
     */
    public MimeBodyPart createAttachment(String fileName) throws Exception {
        MimeBodyPart attachmentPart = new MimeBodyPart();
        FileDataSource fds = new FileDataSource(fileName);
        attachmentPart.setDataHandler(new DataHandler(fds));
        attachmentPart.setFileName(fds.getName());
        return attachmentPart;
    }

    /**
     * 根据传入的邮件正文body和文件路径创建图文并茂的正文部分
     */
    public MimeBodyPart createContent(String body)
            throws Exception {
        // 用于保存最终正文部分
        MimeBodyPart textBody = new MimeBodyPart();
        textBody.setContent(body, "text/html;charset=gbk");
        return textBody;
    }

    /**
     * 根据传入的 Seesion 对象创建混合型的 MIME消息
     */
    public MimeMessage createMessage(Session session,String to) throws Exception {
        String from = userName;
        String subject = send_subject.getText().toString();
        String content = send_content.getText().toString();

        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mc);

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO,to);
        msg.setSubject(subject);
        MimeBodyPart[] attachment = new MimeBodyPart[filePaths.size()];
        MimeMultipart allPart = new MimeMultipart("mixed");
        for(int i = 0;i < filePaths.size();i++) {
            attachment[i] = createAttachment(filePaths.get(i));
            allPart.addBodyPart(attachment[i]);
        }
        MimeBodyPart attachmentContent = createContent(content);
        allPart.addBodyPart(attachmentContent);

        // 将上面混合型的 MimeMultipart 对象作为邮件内容并保存
        msg.setContent(allPart);
        msg.saveChanges();
        return msg;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode ==Activity.RESULT_OK){
            Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。
            String string =uri.toString();
            File file;
            String filePath=getPathByUri4kitkat(this,uri);
           // file=new File(filePath);

            //判断是否有重复文件
            if(!filePaths.contains(filePath))
            {
                filePaths.add(filePath);
                System.out.println(filePath);
                fileNames.add(filePath.substring(filePath.lastIndexOf("/")+1));
                System.out.println(filePath.lastIndexOf("/")+filePath.substring(filePath.lastIndexOf("/"),filePath.length()));
            }

            //更新列表
            myRecyclerAdapter.setmDatas(filePaths);
            myRecyclerAdapter.notifyDataSetChanged();

        }
    }
    // 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
    @SuppressLint("NewApi")
    public static String getPathByUri4kitkat(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {// ExternalStorageProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {// DownloadsProvider
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {// MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {// MediaStore
            // (and
            // general)
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {// File
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context
     *            The context.
     * @param uri
     *            The Uri to query.
     * @param selection
     *            (Optional) Filter used in the query.
     * @param selectionArgs
     *            (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
