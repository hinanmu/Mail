package com.example.nanmu.mail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import org.apache.commons.mail.util.MimeMessageParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;


public class MainActivity extends AppCompatActivity {

    private AccountHeader headerResult = null;
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final InboxContent inboxView = new InboxContent();
        final SentContent sentContent = new SentContent();
        final DraftContent draftContent = new DraftContent();
        final DeleteContent deleteContent = new DeleteContent();


        fragmentManager.beginTransaction().add(R.id.frame_container, inboxView).commit();//设置收件箱界面
        fragmentManager.beginTransaction().add(R.id.frame_container, sentContent).commit();
        fragmentManager.beginTransaction().add(R.id.frame_container, draftContent).commit();
        fragmentManager.beginTransaction().add(R.id.frame_container, deleteContent).commit();
        fragmentManager.beginTransaction().show(inboxView).commit();
        fragmentManager.beginTransaction().hide(sentContent).commit();
        fragmentManager.beginTransaction().hide(draftContent).commit();
        fragmentManager.beginTransaction().hide(deleteContent).commit();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar); //
        setSupportActionBar(toolbar);

        userName = getAccount();//设置显示的账号名字
        final IProfile profile = new ProfileDrawerItem().withName(userName).withIcon(R.drawable.qq).withIdentifier(100);//设置显示账号名
        // Create the AccountHeader
        headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(true)
                .withHeaderBackground(R.color.softblue)
                .addProfiles(
                        profile
                        //don't ask but google uses 14dp for the add account icon in gmail but 20dp for the normal icons (like manage account)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        //sample usage of the onProfileChanged listener
                        //if the clicked item has the identifier 1 add a new profile ;)

                        //false if you have not consumed the event and it should close the drawer
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        PrimaryDrawerItem inbox = new PrimaryDrawerItem().withIdentifier(2).withName("收件箱").withIcon(R.drawable.inbox);//设置item
        PrimaryDrawerItem sent = new PrimaryDrawerItem().withIdentifier(4).withName("已发送").withIcon(R.drawable.archive);
        PrimaryDrawerItem draftBox = new PrimaryDrawerItem().withIdentifier(5).withName("草稿箱").withIcon(R.drawable.drafts);
        PrimaryDrawerItem delete = new PrimaryDrawerItem().withIdentifier(6).withName("已删除").withIcon(R.drawable.delete);
        DividerDrawerItem dividerDrawerItem = new DividerDrawerItem();
        PrimaryDrawerItem about = new PrimaryDrawerItem().withIdentifier(7).withName("关于").withIcon(R.drawable.about);
        PrimaryDrawerItem close = new PrimaryDrawerItem().withIdentifier(8).withName("注销").withIcon(R.drawable.close);

        new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(headerResult)
                .withActionBarDrawerToggleAnimated(true)
                .withTranslucentStatusBar(false)
                .addDrawerItems(
                        new DividerDrawerItem(),
                        inbox,
                        sent,
                        draftBox,
                        delete,
                        dividerDrawerItem,
                        about,
                        close

                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {//设置item的响应操作
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            switch ((int) drawerItem.getIdentifier()) {
                                case 1:
                                    break;
                                case 2:
                                    toolbar.setTitle("收件箱");
                                    fragmentManager.beginTransaction().show( inboxView).commit();
                                    fragmentManager.beginTransaction().hide( sentContent).commit();
                                    fragmentManager.beginTransaction().hide( draftContent).commit();
                                    fragmentManager.beginTransaction().hide(deleteContent).commit();
                                    break;
                                case 4:
                                    toolbar.setTitle("已发送");
                                    fragmentManager.beginTransaction().hide( inboxView).commit();
                                    fragmentManager.beginTransaction().show( sentContent).commit();
                                    fragmentManager.beginTransaction().hide( draftContent).commit();
                                    fragmentManager.beginTransaction().hide(deleteContent).commit();
                                    break;
                                case 5:
                                    toolbar.setTitle("草稿箱");
                                    fragmentManager.beginTransaction().hide( inboxView).commit();
                                    fragmentManager.beginTransaction().hide( sentContent).commit();
                                    fragmentManager.beginTransaction().show( draftContent).commit();
                                    fragmentManager.beginTransaction().hide(deleteContent).commit();
                                    break;
                                case 6:
                                    toolbar.setTitle("已删除");
                                    fragmentManager.beginTransaction().hide( inboxView).commit();
                                    fragmentManager.beginTransaction().hide( sentContent).commit();
                                    fragmentManager.beginTransaction().hide( draftContent).commit();
                                    fragmentManager.beginTransaction().show(deleteContent).commit();
                                    break;
                                case 7:
                                    break;
                                case 8:
                                    closeAccount();
                                    break;
                                default:
                                    break;
                            }
                        }
                        return false;
                    }
                })
                .build();

        try {
            initLoad();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //初始化加载设置账户下载最新十封邮件并保存为eml文档为之后解析
    public void initLoad() throws IOException {
        SharedPreferences mySharedPreferences = getSharedPreferences("userInfo",
                Context.MODE_PRIVATE);
        String userName = mySharedPreferences.getString("userName", "");
        setUserPath(userName);//设置用户路径;
    }

    //设置用户文件路径
    public void setUserPath(String userName) {
        String filePathUser = "/sdcard/SoftMail/" + userName + "/";
        String filePathInbox = "/sdcard/SoftMail/" + userName + "/inbox/";
        String filePathDelete = "/sdcard/SoftMail/" + userName + "/delete/";
        String filePathDraft = "/sdcard/SoftMail/" + userName + "/draft/";
        String filePathSend = "/sdcard/SoftMail/" + userName + "/send/";
        String filePathDoc = "/sdcard/SoftMail/" + userName + "/doc/";

        File fileDirectory1 = new File(filePathUser);
        File fileDirectory2 = new File(filePathInbox);
        File fileDirectory3 = new File(filePathDelete);
        File fileDirectory4 = new File(filePathDraft);
        File fileDirectory5 = new File(filePathSend);
        File fileDirectory6 = new File(filePathDoc);

        if (!fileDirectory1.exists()) {
            fileDirectory1.mkdir();
        }
        if (!fileDirectory2.exists()) {
            fileDirectory2.mkdir();
        }
        if (!fileDirectory3.exists()) {
            fileDirectory3.mkdir();
        }
        if (!fileDirectory4.exists()) {
            fileDirectory4.mkdir();
        }
        if (!fileDirectory5.exists()) {
            fileDirectory5.mkdir();
        }
        if (!fileDirectory6.exists()) {
            fileDirectory6.mkdir();
        }
    }

    //得到帐号名
    public String getAccount() {
        SharedPreferences mySharedPreferences;
        mySharedPreferences = getSharedPreferences("userInfo",
                Context.MODE_PRIVATE);

        Log.d("setA",mySharedPreferences.getString("userName", ""));

        return mySharedPreferences.getString("userName", "");
    }

    //删除当前账号
    public void closeAccount() {
        SharedPreferences mySharedPreferences;
        SharedPreferences.Editor editor;

        mySharedPreferences = getSharedPreferences("userInfo",
                Context.MODE_PRIVATE);
        editor = mySharedPreferences.edit();

        editor.putString("userName", "");
        editor.putString("userPass", "");
        editor.commit();

        Toast.makeText(getApplicationContext(), "已退出" ,
                Toast.LENGTH_LONG).show();

        Intent intent = new Intent(MainActivity.this, UserLogin.class);
        startActivity(intent);
        finish();
    }


}
