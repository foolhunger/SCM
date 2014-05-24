package com.ebao.scm.tools;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.Properties;
import org.apache.axis.AxisFault;
import com.collabnet.ce.soap60.webservices.ClientSoapStubFactory;
import com.collabnet.ce.soap60.webservices.cemain.ICollabNetSoap;
import com.collabnet.ce.soap60.webservices.cemain.UserSoapDO;
import com.collabnet.ce.soap60.webservices.tracker.ArtifactSoapDO;
import com.collabnet.ce.soap60.webservices.tracker.ITrackerAppSoap;

public final class CIMailCTFUtils extends CIMailLogger {
	public CIMailCTFUtils() {
		try {
			InputStream ins = CIMailCTFUtils.class.getResourceAsStream("CIMailRobot.properties");
			if (ins == null) {
				System.out.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailCTFUtils()]");
				errLogger.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailCTFUtils()]");
				houseKeeping();
				System.exit(-1);
			}
			Properties props = new Properties();
			props.load(ins);
			ctfURL = props.getProperty("ctf.url", "").trim();
			if (ctfURL.isEmpty()) {
				System.out.println("==> Oops, failed to read property [ctf.url] [CIMailRobot.properites]");
				errLogger.println("==> Oops, failed to read property [ctf.url] [CIMailRobot.properites]");
				houseKeeping();
				System.exit(-1);
			}
			ctfAuthUsername = props.getProperty("ctf.auth.username", "").trim();
			if (ctfAuthUsername.isEmpty()) {
				System.out.println("==> Oops, failed to read property [ctf.auth.username] [CIMailRobot.properites]");
				errLogger.println("==> Oops, failed to read property [ctf.auth.username] [CIMailRobot.properites]");
				houseKeeping();
				System.exit(-1);
			}
			ctfAuthPassword = props.getProperty("ctf.auth.password", "").trim();
			if (ctfAuthPassword.isEmpty()) {
				System.out.println("==> Oops, failed to read property [ctf.auth.password] [CIMailRobot.properites]");
				errLogger.println("==> Oops, failed to read property [ctf.auth.password] [CIMailRobot.properites]");
				houseKeeping();
				System.exit(-1);
			}
			
			ctfCemainSoap = (ICollabNetSoap) ClientSoapStubFactory.getSoapStub(ICollabNetSoap.class, ctfURL);
			ctfTrackerSoap = (ITrackerAppSoap) ClientSoapStubFactory.getSoapStub(ITrackerAppSoap.class, ctfURL);
		}
		catch (IOException e) {
			System.out.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailCTFUtils()]");
			errLogger.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailCTFUtils()]");
			e.printStackTrace();
			houseKeeping();
			System.exit(-1);
		}
		catch (Throwable e) {
			System.out.println("==> Oops, failed to instantiate CTF SOAP objects [" + e.getMessage() + "]");
			errLogger.println("==> Oops, failed to instantiate CTF SOAP objects [" + e.getMessage() + "]");
			e.printStackTrace();
			houseKeeping();
			System.exit(-1);
		}
	}
	
	public void login() {
		try {
			ctfSessionId = ctfCemainSoap.login(ctfAuthUsername, ctfAuthPassword);
		}
		catch (RemoteException e) {
			String faultString = ((AxisFault)e).getFaultString();
			System.out.println("==> Oops, failed to login to Teamforge [" + ctfURL + "] [" + ctfAuthUsername + "]");
			System.out.println("==> Cause: " + faultString);
			errLogger.println("==> Oops, failed to login to Teamforge [" + ctfURL + "] [" + ctfAuthUsername + "]");
			errLogger.println("==> Cause: " + faultString);
			e.printStackTrace();
			houseKeeping();
			System.exit(-1);
		}
		System.out.println("==> Teamforge: logged in as user [" + ctfAuthUsername + "]");
		stdLogger.println("==> Teamforge: logged in as user [" + ctfAuthUsername + "]");
	}
	
	public void logoff() {
		try {
			ctfCemainSoap.logoff(ctfAuthUsername, ctfSessionId);
		}
		catch (RemoteException e) {
			String faultString = ((AxisFault)e).getFaultString();
			System.out.println("==> Oops, failed to logoff from Teamforge [" + ctfURL + "] [" + ctfAuthUsername + "]");
			System.out.println("==> Cause: " + faultString);
			errLogger.println("==> Oops, failed to logoff from Teamforge [" + ctfURL + "] [" + ctfAuthUsername + "]");
			errLogger.println("==> Cause: " + faultString);
			e.printStackTrace();
			houseKeeping();
			System.exit(-1);
		}
		System.out.println("==> Teamforge: logged off as user [" + ctfAuthUsername + "]");
		stdLogger.println("==> Teamforge: logged off as user [" + ctfAuthUsername + "]");
	}
	
	public String getArtifactTitle(String artifactId) {
		if (artifactId == null) {
			System.out.println("==> Oops, artifact id cannot be null [CIMailCTFUtils.getArtifactTitle()]");
			errLogger.println("==> Oops, artifact id cannot be null [CIMailCTFUtils.getArtifactTitle()]");
			houseKeeping();
			System.exit(-1);
		}
		String artifactTitle = "";
		try {
			ArtifactSoapDO artifactSoapDo = ctfTrackerSoap.getArtifactData(ctfSessionId, artifactId.trim());
			artifactTitle = artifactSoapDo.getTitle();
		}
		catch (RemoteException e) {
			String faultString = ((AxisFault)e).getFaultString();
			System.out.println("==> Oops, failed to retrieve artifact title [" + artifactId + "] [CIMailCTFUtils.getArtifactTitle()]");
			System.out.println("==> Cause: " + faultString);
			errLogger.println("==> Oops, failed to retrieve artifact title [" + artifactId + "] [CIMailCTFUtils.getArtifactTitle()]");
			errLogger.println("==> Cause: " + faultString);
			return null;
		}
		return artifactTitle;
	}
	
	public String getUserFullName(String username) {
		if (username == null) {
			System.out.println("==> Oops, username cannot be null [CIMailCTFUtils.getUserFullName()]");
			errLogger.println("==> Oops, username cannot be null [CIMailCTFUtils.getUserFullName()]");
			houseKeeping();
			System.exit(-1);
		}
		String userFullName = "";
		try {
			UserSoapDO userSoapDo = ctfCemainSoap.getUserData(ctfSessionId, username.trim());
			userFullName = userSoapDo.getFullName();
		}
		catch (RemoteException e) {
			String faultString = ((AxisFault)e).getFaultString();
			System.out.println("==> Oops, failed to retrieve user fullname [" + username + "] [CIMailCTFUtils.getUserFullName()]");
			System.out.println("==> Cause: " + faultString);
			errLogger.println("==> Oops, failed to retrieve user fullname [" + username + "] [CIMailCTFUtils.getUserFullName()]");
			errLogger.println("==> Cause: " + faultString);
			return null;
		}
		return userFullName;
	}
	
	public String getUserEmail(String username) {
		if (username == null) {
			System.out.println("==> Oops, username cannot be null [CIMailCTFUtils.getUserEmail()]");
			errLogger.println("==> Oops, username cannot be null [CIMailCTFUtils.getUserEmail()]");
			houseKeeping();
			System.exit(-1);
		}
		String userEmail = "";
		try {
			UserSoapDO userSoapDo = ctfCemainSoap.getUserData(ctfSessionId, username.trim());
			userEmail = userSoapDo.getEmail();
		}
		catch (RemoteException e) {
			String faultString = ((AxisFault)e).getFaultString();
			System.out.println("==> Oops, failed to retrieve user email [" + username + "] [CIMailCTFUtils.getUserEmail()]");
			System.out.println("==> Cause: " + faultString);
			errLogger.println("==> Oops, failed to retrieve user email [" + username + "] [CIMailCTFUtils.getUserEmail()]");
			errLogger.println("==> Cause: " + faultString);
			return null;
		}
		return userEmail;
	}
	
	@Override
	public void houseKeeping() {
		stdLogger.close();
		errLogger.close();
	}
	
	private PrintWriter stdLogger = stdLoggerFactory();
	private PrintWriter errLogger = errLoggerFactory();
	private String ctfURL;
	private String ctfAuthUsername;
	private String ctfAuthPassword;
	private String ctfSessionId;
	private ICollabNetSoap ctfCemainSoap;
	private ITrackerAppSoap ctfTrackerSoap;
}