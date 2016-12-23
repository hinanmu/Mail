package com.example.nanmu.mail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;

public class UserLogin extends AppCompatActivity {

    @Bind(R.id.input_email)
    EditText inputEmail;
    @Bind(R.id.input_password)
    EditText inputPassword;
    private AppCompatButton btnLogin;

    private SharedPreferences mySharedPreferences;
    private SharedPreferences.Editor  editor;
    private  MyHandler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_login);
        ButterKnife.bind(this);

        btnLogin = (AppCompatButton)findViewById(R.id.btn_login) ;


        mySharedPreferences = getSharedPreferences("userInfo",
                Context.MODE_PRIVATE);
        editor = mySharedPreferences.edit();

        if (mySharedPreferences.getString("userName","") !="" && mySharedPreferences.getString("userPass","") !="")
        {
            Toast.makeText(getApplicationContext(), "登录成功" ,
                    Toast.LENGTH_LONG).show();

            Intent intent = new Intent(UserLogin.this,MainActivity.class) ;
            startActivity(intent) ;
            finish();
        }
        else
        {
            myHandler = new MyHandler();

            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                setAppPath();
                                setAccount();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }).start();
                }
            });
        }
    }

    public void setAccount() throws IOException, InterruptedException {

        if(inputEmail.getText().length() > 5 && inputPassword.getText().length() > 0 && inputEmail.getText().toString().contains("@"))
        {

            String pop3Server = "pop."+inputEmail.getText().toString().split("@")[1];
            String userName = inputEmail.getText().toString();
            String userPass = inputPassword.getText().toString();
            Boolean connect ;

            POP3Demo pop3Demo = new POP3Demo(pop3Server,userName,userPass);
            connect = pop3Demo.connect();

            Thread.sleep(1000);

            if(connect) {
                editor.putString("userName", userName);
                editor.putString("userPass", userPass);
                editor.commit();
                sendMsg("true");
            }
            else {
                sendMsg("false");
            }
        }
        else {
            sendMsg("false");
        }
    }

    public void setAppPath()
    {
        String filePath = "/sdcard/SoftMail/";
        File fileDirectory = new File(filePath);

        if(!fileDirectory.exists()) {
            fileDirectory.mkdir();
        }
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
                Toast.makeText(getApplicationContext(), "登录成功" ,
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(UserLogin.this,MainActivity.class) ;
                startActivity(intent) ;
                finish();
            }
            else {
                Toast.makeText(getApplicationContext(), "请检查用户名和密码" ,
                        Toast.LENGTH_LONG).show();
            }
            ;
        }
    }

}
