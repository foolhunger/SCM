package com.ebao.scm.tools;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;

final class CIMailSVNLogEntryHandler implements ISVNLogEntryHandler {
	public CIMailSVNLogEntryHandler() {}
	
	public String getAuthor() {
		return author;
	}
	
	public String getMessage() {
		return message;
	}
	
	public long getRevision() {
		return revision;
	}
	
	@Override
	public void handleLogEntry(SVNLogEntry logEntry) {
		author = logEntry.getAuthor().trim();
		message = logEntry.getMessage().trim();
		revision = logEntry.getRevision();
	}
	
	private String author;
	private String message;
	private long revision;
}

public final class CIMailSVNUtils extends CIMailLogger {
	public CIMailSVNUtils() {
		try {
			InputStream ins = CIMailSVNUtils.class.getResourceAsStream("CIMailRobot.properties");
			if (ins == null) {
				System.out.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailSVNUtils()]");
				errLogger.println("==> Oops, failed to read property file [CIMailRobot.properites] [CIMailSVNUtils()]");
				houseKeeping();
				System.exit(-1);
			}
			Properties props = new Properties();
			props.load(ins);
			svnAuthUsername = props.getProperty("svn.auth.username", "").trim();
			if (svnAuthUsername.isEmpty()) {
				System.out.println("==> Oops, failed to read property [svn.auth.username] [CIMailRobot.properites]");
				errLogger.println("==> Oops, failed to read property [svn.auth.username] [CIMailRobot.properites]");
				houseKeeping();
				System.exit(-1);
			}
			svnAuthPassword = props.getProperty("svn.auth.password", "").trim();
			if (svnAuthPassword.isEmpty()) {
				System.out.println("==> Oops, failed to read property [svn.auth.password] [CIMailRobot.properites]");
				errLogger.println("==> Oops, failed to read property [svn.auth.password] [CIMailRobot.properites]");
				houseKeeping();
				System.exit(-1);
			}
		}
		catch (IOException e) {
			System.out.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailSVNUtils()]");
			errLogger.println("==> Oops, failed to load properties [" + e.getMessage() + "] [CIMailSVNUtils()]");
			e.printStackTrace();
			houseKeeping();
			System.exit(-1);
		}
	}
	
	public String getWCLastCommitAuthorRevisionAndMessage(String filename) {
		SVNClientManager clientMgr = SVNClientManager.newInstance(null, svnAuthUsername, svnAuthPassword);
		SVNLogClient logClient = clientMgr.getLogClient();
		String author = "";
		String message = "";
		long revision = 0;
		try {
			File[] files = new File[1];
			files[0] = new File(filename.trim());
			CIMailSVNLogEntryHandler logEntryHandler = new CIMailSVNLogEntryHandler();
			logClient.doLog(files, null, null, true, true, 1L, logEntryHandler);
			author = logEntryHandler.getAuthor();
			message = logEntryHandler.getMessage();
			revision = logEntryHandler.getRevision();
		}
		catch (SVNException e) {
			System.out.println("==> Oops, failed to retrieve SVN info [" + e.getMessage() + "]");
			errLogger.println("==> Oops, failed to retrieve SVN info [" + e.getMessage() + "]");
			e.printStackTrace();
			houseKeeping();
			System.exit(-1);
		}
		System.out.println("==> >>>>> [" + author + "] [" + revision + "] {" + message + "}");
		stdLogger.println("==> >>>>> [" + author + "] [" + revision + "] {" + message + "}");
		return author + ":" + revision + ":" + message;
	}
	
	@Override
	public void houseKeeping() {
		stdLogger.close();
		errLogger.close();
	}
	
	private PrintWriter stdLogger = stdLoggerFactory();
	private PrintWriter errLogger = errLoggerFactory();
	private String svnAuthUsername;
	private String svnAuthPassword;
}