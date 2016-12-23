package com.example.nanmu.mail;


import android.os.Environment;
import android.util.Log;

import org.apache.commons.mail.util.MimeMessageParser;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static android.content.Context.MODE_PRIVATE;


public class POP3Demo {
	private String pop3Server = "";
	private String userName = "";
	private String userPass = "";
	private int port = 995;
	private int inboxCurrentNum = 0;
	private ArrayList<InboxInfo> inboxInfos = new ArrayList<>();
	private SSLSocket socket;
	private DataInputStream input;
	private DataOutputStream output;
	private String response;

	public POP3Demo(String pop3Server, String userName, String userPass) {
		this.pop3Server = pop3Server;
		this.userName = userName;
		this.userPass = userPass;
	}

	@SuppressWarnings("deprecation")
	public boolean connect() throws UnknownHostException, IOException// 通过之前设定的pop地址，端口，用户名，密码进行连接，如果连接成功则isUserConnect														// = true
	{
		try {
			socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(pop3Server, 995);

			input = new DataInputStream(socket.getInputStream());
			output = new DataOutputStream(socket.getOutputStream());

			if (input.readLine().startsWith("+OK")) {
				System.out.println("连接成功");
			}

			setResponse("user " + this.userName);
			setResponse("pass " + this.userPass);

			if (response.startsWith("+OK")) {
				return true;
			} else {
				return false;
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void setInboxNum() throws IOException // 设置当前邮件总数的值
	{
		try {
			setResponse("stat");
			inboxCurrentNum = Integer.valueOf(response.split(" ")[1]);
			System.out.println("当前邮件总数:" + inboxCurrentNum);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getInboxNum() throws IOException // 设置当前邮件总数的值
	{
		try {
			setResponse("stat");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Integer.valueOf(response.split(" ")[1]);
	}

	public void setResponse(String request) throws IOException // 通过传入一个请求，获取回复，并设置response变量
	{
		try {
			String responseLine = "";
			output.writeBytes(request + "\r\n");

			if (request.startsWith("retr") || request.startsWith("uidl")) {
				input.readLine();
				responseLine = input.readLine();
				response = "";
				while (responseLine.toLowerCase().equals(".") != true) {
					response = response + responseLine + '\n';
					responseLine = input.readLine();
				}
				response = response + '.';
			} else {
				response = input.readLine();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void refresh() throws IOException// 刷新当前的邮件数
	{
		setInboxNum();
	}


	public void downloadMail(int i) throws IOException {
		String filePath = "/sdcard/SoftMail/" + userName + "/inbox";
		String fileName = getNumToUid().get(i) + ".eml";

		setResponse("retr " + i);
		try {
			File fileDirectory = new File(filePath);

			if (!fileDirectory.exists()) {
				fileDirectory.mkdir();
			}
			File outputFile = new File(fileDirectory, fileName);
			if (!outputFile.exists()) {
				FileOutputStream fos = new FileOutputStream(outputFile);
				fos.write(response.getBytes());
				fos.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Map<Integer, String> getNumToUid() throws IOException {


		Integer mailNum;
		String mailUid;
		Map<Integer, String> mapUid = new HashMap();

		setResponse("uidl");

		for (int i = 0; i < response.split("\n").length - 1; i++) {
			mailNum = Integer.valueOf(response.split("\n")[i].split(" ")[0]);
			mailUid = response.split("\n")[i].split(" ")[1];
			mapUid.put(mailNum, mailUid);
		}
		return mapUid;
	}

	public Map<String, Integer> getUidToNum() throws IOException {
		Map<Integer, String> mapUid = getNumToUid();
		Map<String, Integer> mapNum = new HashMap();

		for(Map.Entry<Integer, String> entry:mapUid.entrySet()){
			mapNum.put(entry.getValue(),entry.getKey());
		}
		return mapNum;
	}

	//public void getInboxContent() throws Exception {
//
//		InboxInfo inboxInfo = new InboxInfo();
//		for(int i = inboxCurrentNum; i > inboxCurrentNum - 25;i--)
//		{
//			setResponse("retr "+i);
//			String mes = response;
//			Session s = Session.getDefaultInstance(new Properties());
//			InputStream is = new ByteArrayInputStream(mes.getBytes());
//			MimeMessage message = new MimeMessage(s, is);
//			MimeMessageParser parser = new MimeMessageParser((MimeMessage) message).parse();
//			String htmlContent = parser.getHtmlContent(); // 获取Html内容
//
//			inboxInfo.setFrom(getFrom(message));
//			inboxInfo.setDate(getSentDate(message, ""));
//			inboxInfo.setId(i);
//			inboxInfo.setContent(htmlContent);
//			inboxInfo.setSubject(getSubject(message));
//			inboxInfos.add(inboxInfo);
//		}
//
//		for(int i = 0;i < inboxInfos.size();i++)
//		{
//			System.out.println(inboxInfos.get(i).getFrom());
//			System.out.println(inboxInfos.get(i).getSubject());
//			System.out.println(inboxInfos.get(i).getDate());
//			System.out.println(inboxInfos.get(i).getId());
//			System.out.println(inboxInfos.get(i).getContent());
//		}
//		System.out.println("获取完成");
//		//String plainContent = parser.getPlainContent();
//
//
//	}

//	// 获得邮件主题
//	public String getSubject(MimeMessage msg) throws UnsupportedEncodingException, MessagingException {
//		return MimeUtility.decodeText(msg.getSubject());
//	}
//
//	// 获得邮件发件人
//	public String getFrom(MimeMessage msg) throws MessagingException, UnsupportedEncodingException {
//		String from = "";
//		Address[] froms = msg.getFrom();
//		if (froms.length < 1)
//			throw new MessagingException("没有发件人!");
//
//		InternetAddress address = (InternetAddress) froms[0];
//		String person = address.getPersonal();
//		if (person != null) {
//			person = MimeUtility.decodeText(person) + " ";
//		} else {
//			person = "";
//		}
//		from = person + "<" + address.getAddress() + ">";
//
//		return from;
//	}
//
//	// 获得发件日期
//	public String getSentDate(MimeMessage msg, String pattern) throws MessagingException {
//		Date receivedDate = msg.getSentDate();
//		if (receivedDate == null)
//			return "";
//
//		if (pattern == null || "".equals(pattern))
//			pattern = "yyyy年MM月dd日 E HH:mm ";
//
//		return new SimpleDateFormat(pattern).format(receivedDate);
//	}

//	public void getMailContent(Part part, StringBuffer content) throws Exception {
//		String contenttype = part.getContentType();
//		int nameindex = contenttype.indexOf("name");
//		boolean conname = false;
//		if (nameindex != -1)
//			conname = true;
//		if (part.isMimeType("text/plain") && !conname) {
//			content.append((String) part.getContent());
//		} else if (part.isMimeType("text/html") && !conname) {
//			content.append((String) part.getContent());
//		} else if (part.isMimeType("multipart/*")) {
//			Multipart multipart = (Multipart) part.getContent();
//			int counts = multipart.getCount();
//			for (int i = 0; i < counts; i++) {
//				getMailContent(multipart.getBodyPart(i), content);
//			}
//		} else if (part.isMimeType("message/rfc822")) {
//			getMailContent((Part) part.getContent(), content);
//		} else {
//		}
//	}

//	public void getMailTextContent(Part part, StringBuffer content) throws MessagingException, IOException {
//		// 如果是文本类型的附件，通过getContent方法可以取到文本内容，但这不是我们需要的结果，所以在这里要做判断
//		boolean isContainTextAttach = part.getContentType().indexOf("name") > 0;
//		if (part.isMimeType("text/*") && !isContainTextAttach) {
//			content.append(part.getContent().toString());
//		} else if (part.isMimeType("message/rfc822")) {
//			getMailTextContent((Part) part.getContent(), content);
//		} else if (part.isMimeType("multipart/*")) {
//			Multipart multipart = (Multipart) part.getContent();
//			int partCount = multipart.getCount();
//			for (int i = 0; i < partCount; i++) {
//				BodyPart bodyPart = multipart.getBodyPart(i);
//				getMailTextContent(bodyPart, content);
//			}
//		}
//	}
}
