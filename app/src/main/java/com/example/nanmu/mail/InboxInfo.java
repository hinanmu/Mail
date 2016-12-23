package com.example.nanmu.mail;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import org.apache.commons.mail.util.MimeMessageParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

public class InboxInfo {

    private String from;
    private String date;
    private String subject;
    private String content;
    private Integer id;
    private Integer total;
    private String uid;

    public InboxInfo() {
    }

    public InboxInfo(String from, String date, String subject, String content) {
        this.from = from;
        this.date = date;
        this.subject = subject;
        this.content = content;
    }

    public String getFrom() {
        return from;
    }

    public String getDate() {
        return date;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public void parseAll(MimeMessage message ) throws Exception {
        MimeMessageParser parser = new MimeMessageParser((MimeMessage) message).parse();

        List<DataSource> attachments = parser.getAttachmentList();
        for (DataSource ds : attachments) {
            BufferedOutputStream outStream = null;
            BufferedInputStream ins = null;
            try {
                String fileName = "/sdcard/SoftMail/" + File.separator + ds.getName();
                outStream = new BufferedOutputStream(new FileOutputStream(fileName));
                ins = new BufferedInputStream(ds.getInputStream());
                byte[] data = new byte[2048];
                int length = -1;
                while ((length = ins.read(data)) != -1) {
                    outStream.write(data, 0, length);
                }
                outStream.flush();
                System.out.println("附件:" + fileName);
            } finally {
                if (ins != null) {
                    ins.close();
                }
                if (outStream != null) {
                    outStream.close();
                }
            }
        }
    }


}
