package com.example.nanmu.mail;

/**
 * Created by nanmu on 2016/12/13.
 */
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.lang.StringUtils;


public class ParseEml implements Comparable{



    public String getPersonalAdd() {
        return personalAdd;
    }

    public String getPersonalName() {
        return personalName;
    }

    public String getMailSubject() {
        return mailSubject;
    }

    public String getPlainContent() {
        return plainContent;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public Date getDate() {
        return date;
    }

    public String getUid() {
        return uid;
    }

    public List<String> getFileName() {
        return fileName;
    }

    public String getUserName() {
        return userName;
    }

    public Message getMsg() {
        return msg;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientAdd() {
        return recipientAdd;
    }

    private String personalAdd = "";
    private String personalName = "";



    private String recipientAdd = "";
    private String recipientName = "";
    private String mailSubject = "";
    private String plainContent = "";
    private String htmlContent = "";
    private Date date = new Date();
    private String uid = "";
    private List<String> fileName;
    private String emlPath;



    private Message msg;


    private String userName;

    public ParseEml(String userName)
    {
        this.userName = userName;
        fileName = new ArrayList();

    }


    public  void parserFile(String emlPath) throws Exception {
        this.emlPath = emlPath;

        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mc);

        System.out.println(emlPath);
        uid = emlPath.split("/")[5].substring(0,emlPath.split("/")[5].length()-4);
        System.out.println(uid);
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        InputStream inMsg;
        inMsg = new FileInputStream(emlPath);
        msg = new MimeMessage(session, inMsg);
        parseEml();
    }

    private  void parseEml() throws Exception {
        // 发件人信息
        Address[] froms = msg.getFrom();
        if (froms != null) {
            // System.out.println("发件人信息:" + froms[0]);
            InternetAddress addr = (InternetAddress) froms[0];
            System.out.println("发件人地址:" + addr.getAddress());
            System.out.println("发件人显示名:" + addr.getPersonal());
            personalAdd  = addr.getAddress();
            personalName = addr.getPersonal();
        }

        Address[] recipients = msg.getAllRecipients();
        if (recipients != null) {
            // System.out.println("发件人信息:" + froms[0]);
            InternetAddress addr = (InternetAddress) recipients[0];
            System.out.println("发件人地址:" + addr.getAddress());
            System.out.println("发件人显示名:" + addr.getPersonal());
            recipientAdd  = addr.getAddress();
            recipientName = addr.getPersonal();
        }

        System.out.println("邮件主题:" + msg.getSubject());
        mailSubject = msg.getSubject();
        System.out.println("发件时间:" + msg.getSentDate());
        date = msg.getSentDate();
        if(date == null)
        {
            String[] temp = msg.getHeader("Received");
            if(temp == null|| temp.length < 1)
            {
                date = new Date(System.currentTimeMillis());
            }
            else {
                String dateStr = temp[0];
                date = new Date(dateStr.split(";")[1]);
            }

            System.out.println("发件时间:" + date);
//            if (date == null)
//            {
//                date = new Date();
//            }
        }

    }


    public void parseEmlFile() throws Exception {

        // getContent() 是获取包裹内容, Part相当于外包装
        Object o = msg.getContent();

        System.out.println("fafafafafafafafafafaafafafafaf");

        if (o instanceof Multipart) {
            Multipart multipart = (Multipart) o ;
            System.out.println("fafafafafafafafafafaafafafafaf");
            reMultipart(multipart);
        } else if (o instanceof Part) {
            Part part = (Part) o;
            rePart(part);
        } else {
             System.out.println("类型" + msg.getContentType());
            System.out.println("内容" + msg.getContent());
        }
    }

    private void rePart(Part part) throws Exception {

        if (part.getDisposition() != null) {

            String strFileNmae = part.getFileName();
            if(!StringUtils.isEmpty(strFileNmae))
            {	// MimeUtility.decodeText解决附件名乱码问题
                strFileNmae=MimeUtility.decodeText(strFileNmae);
                System.out.println("发现附件: "+ strFileNmae);

                InputStream in = part.getInputStream();// 打开附件的输入流
                // 读取附件字节并存储到文件中
                java.io.FileOutputStream out = new FileOutputStream("/sdcard/SoftMail/"+userName+"/doc/"+strFileNmae);
                fileName.add(strFileNmae);
                int data;
                while ((data = in.read()) != -1) {
                    out.write(data);
                }
                in.close();
                out.close();

            }

            System.out.println("内容类型: "+ MimeUtility.decodeText(part.getContentType()));
            System.out.println("附件内容:" + part.getContent());


        } else {
            if (part.getContentType().startsWith("text/plain")) {
                plainContent = part.getContent().toString();
                System.out.println("文本内容：" + part.getContent());
            } else {
                htmlContent = part.getContent().toString();
                System.out.println("HTML内容：" + part.getContent());
            }
        }
    }


    /**
     * @param multipart
     *            // 接卸包裹（含所有邮件内容(包裹+正文+附件)）
     * @throws Exception
     */
    private void reMultipart(Multipart multipart) throws Exception {
        // System.out.println("邮件共有" + multipart.getCount() + "部分组成");
        // 依次处理各个部分
        for (int j = 0, n = multipart.getCount(); j < n; j++) {
            // System.out.println("处理第" + j + "部分");
            Part part = multipart.getBodyPart(j);// 解包, 取出 MultiPart的各个部分,
            // 每部分可能是邮件内容,
            // 也可能是另一个小包裹(MultipPart)
            // 判断此包裹内容是不是一个小包裹, 一般这一部分是 正文 Content-Type: multipart/alternative
            if (part.getContent() instanceof Multipart) {
                Multipart p = (Multipart) part.getContent();// 转成小包裹
                // 递归迭代
                reMultipart(p);
            } else {
                rePart(part);
            }
        }
    }


    public  String getMailContent(Part part) throws Exception {
        String contenttype = part.getContentType();
        int nameindex = contenttype.indexOf("name");
        boolean conname = false;
        if (nameindex != -1) {
            conname = true;
        }
        StringBuilder bodytext = new StringBuilder();
        if (part.isMimeType("text/plain") && !conname) {
            bodytext.append((String) part.getContent());
        } else if (part.isMimeType("text/html") && !conname) {
            bodytext.append((String) part.getContent());
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            int counts = multipart.getCount();
            for (int i = 0; i < counts; i++) {
                getMailContent(multipart.getBodyPart(i));
            }
        } else if (part.isMimeType("message/rfc822")) {
            getMailContent((Part) part.getContent());
        } else {
        }
        return bodytext.toString();
    }

    @Override
    public int compareTo(Object o) {
        ParseEml p = (ParseEml)o;
        if(this.date.before(p.date))
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }


    // @Override
//    public int compareTo(Object o) {
//        if(o instanceof ParseEml){
//            ParseEml p=(ParseEml) o;
//            if(this.date.after(p.date)){
//                return 1;
//            }
//            else{
//                return 0;
//            }
//        }
//        return -1;
//    }
}