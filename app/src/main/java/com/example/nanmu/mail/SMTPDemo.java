package com.example.nanmu.mail;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SMTPDemo {
	private String mailServer;
	private String userName;
	private String userPass;
	private String to;
	private String content;
	private String lineFeet = "\r\n";
	private int port = 465;
	private SSLSocket client;
	private BufferedReader in;
	private DataOutputStream os;

	public SMTPDemo(String userName,String userPass ,String to,String content)
	{
		this.userName = userName;
		this.userPass = userPass;
		this.content = content;
		this.to = to;
		this.mailServer = "smtp."+userName.split("@")[1];
	}


	public boolean connect() {
		boolean boo = true;
		if (mailServer == null || "".equals(mailServer)) {
			return false;
		}
		try {

			client=(SSLSocket) SSLSocketFactory.getDefault().createSocket(mailServer, port);

			in = new BufferedReader(new InputStreamReader(
					client.getInputStream()));
			os = new DataOutputStream(client.getOutputStream());
			String isConnect = response();
			if (isConnect.startsWith("220")) {
				System.out.println("建立连接成功："+isConnect);
			} else {
				System.out.println("建立连接失败：" + isConnect);
				boo = false;
			}
		} catch (UnknownHostException e) {
			System.out.println("建立连接失败！");
			e.printStackTrace();
			boo = false;
		} catch (IOException e) {
			System.out.println("读取流失败！");
			e.printStackTrace();
			boo = false;
		}
		return boo;
	}

	private String sendCommand(String msg) {
		String result = null;
		try {
			os.writeBytes(msg);
			os.flush();
			result = response();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	private String response() {
		String result = null;
		try {
			result = in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	private void close() {
		try {
			os.close();
			in.close();
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public boolean sendMail() {


		String result = sendCommand("HELO " + mailServer + lineFeet);
		if (isStartWith(result, "250")) {
			System.out.println("握手成功：" + result);
		} else {
			System.out.println("握手失败：" + result);
			return false;
		}

		String auth = sendCommand("AUTH LOGIN" + lineFeet);
		if (isStartWith(auth, "334")) {
			System.out.println("开始输入账号或密码：" + auth);
		} else {
			System.out.println("输入失败：" + auth);
			return false;
		}

		String user = sendCommand(new String(Base64.encodeToString(userName.getBytes(),Base64.DEFAULT))
				+ lineFeet);
		if (isStartWith(user, "334")) {
			System.out.println("开始输入账号或密码：" + user);
		} else {
			System.out.println("输入失败：" +user);
			return false;
		}
		String p = sendCommand(new String(Base64.encodeToString(userPass.getBytes(),Base64.DEFAULT))
				+ lineFeet);
		if (isStartWith(p, "235")) {
			System.out.println("登录成功");
		} else {
			return false;
		}

		String f = sendCommand("Mail From:<" + userName + ">" + lineFeet);
		if (isStartWith(f, "250")) {
			System.out.println("250 ok");
		} else {
			return false;
		}
		String toStr = sendCommand("RCPT TO:<" + to + ">" + lineFeet);
		if (isStartWith(toStr, "250")) {
			System.out.println("250 ok");
		} else {
			return false;
		}

		String data = sendCommand("DATA" + lineFeet);
		if (isStartWith(data, "354")) {
			System.out.println("输入内容");
		} else {
			return false;
		}

		String conStr = sendCommand(content+lineFeet+"."+lineFeet);//发送邮件内容
		if (isStartWith(conStr, "250")) {
			System.out.println("输入内容完成");
		} else {
			return false;
		}

		String quit = sendCommand("QUIT" + lineFeet);
		if (isStartWith(quit, "221")) {
		} else {
			return false;
		}
		close();
		return true;
	}

	private boolean isStartWith(String res, String with) {
		return res.startsWith(with);
	}


}