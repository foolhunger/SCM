package com.ebao.scm.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;

public final class CIMailParser extends CIMailLogger {
	public CIMailParser() {
		compileErrLogs = new ArrayList<String>(50);
		compileErrDetails = new HashMap<String, String>(50);
	}
	
	public boolean parse(String clog, String prefix) {
		if (clog == null) {
			System.out.println("==> Oops, clog cannot be null [CIMailParser.parse()]");
			errLogger.println("==> Oops, clog cannot be null [CIMailParser.parse()]");
			houseKeeping();
			System.exit(-1);
		}
		if (prefix == null) {
			System.out.println("==> Oops, prefix cannot be null [CIMailParser.parse()]");
			errLogger.println("==> Oops, prefix cannot be null [CIMailParser.parse()]");
			houseKeeping();
			System.exit(-1);
		}
		compileErrLogs.clear();
		compileErrDetails.clear();
		try {
			clog = clog.replaceAll("\\\\", "/").trim();
			FileUtils.copyFile(new File(clog), new File("clog.txt"));
			Scanner sin = new Scanner(new File("clog.txt"));
			// parse for compilation error logs
			Pattern ptnBOE = Pattern.compile("(?:\\[(?:javac|info)\\]\\s*compiling\\s+\\d+)|(?:\\[error\\]\\s*failed\\s+to.+compilation\\s+failure)", Pattern.CASE_INSENSITIVE);
			Pattern ptnEOE = Pattern.compile("(?:\\[(?:javac|info)\\]\\s*\\d+\\s*error)|(?:\\[error\\]\\s*->\\s*\\[\\s*help\\s*\\d+\\s*\\])|(?:\\[(?:java|info)\\]\\s*build\\s+(?:failed|failure))", Pattern.CASE_INSENSITIVE);
			boolean flag = false;
			while (sin.hasNextLine()) {
				String line = sin.nextLine().trim();
				Matcher mtrBOE = ptnBOE.matcher(line);
				Matcher mtrEOE = ptnEOE.matcher(line);
				if (mtrBOE.find()) {
					compileErrLogs.clear();
				}
				compileErrLogs.add(line);
				if (mtrEOE.find()) {
					if (!compileErrLogs.isEmpty()) {
						flag = true;
					}
					break;
				}
			}
			if (!flag) {
				System.out.println("==> No compilation errors have been found [" + clog + "]");
				stdLogger.println("==> No compilation errors have been found [" + clog + "]");
				return false;
			}
			// parse for compilation error files
			prefix = prefix.replaceAll("\\\\", "/").trim();
			Pattern pattern = Pattern.compile(".+?\\/([a-zA-Z]\\$\\/.+?)\\/*\\Z", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(prefix);
			String localPath = "";
			if (matcher.find()) {
				localPath = matcher.group(1).replaceFirst("\\$", "\\:");
				stdLogger.println("==> local path is [" + localPath + "] [CIMailParser.parse()]");
			}
			else {
				System.out.println("==> Oops, path format is incorrent [" + prefix + "] [CIMailParser.parse()]");
				errLogger.println("==> Oops, path format is incorrent [" + prefix + "] [CIMailParser.parse()]");
				System.exit(-1);
			}
			pattern = Pattern.compile("\\[(?:javac|error)\\].*?\\Q" + localPath + "\\E\\/(.+?)\\s*\\:", Pattern.CASE_INSENSITIVE);
			flag = false;
			for (String line: compileErrLogs) {
				line = line.replaceAll("\\\\", "/").trim();
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					if (!compileErrDetails.containsKey(matcher.group(1))) {
						System.out.println("==> ##### [" + matcher.group(1) + "]");
						stdLogger.println("==> ##### [" + matcher.group(1) + "]");
						compileErrDetails.put(matcher.group(1), "N/A");
					}
					flag = true;
				}
			}
			if (!flag) {
				System.out.println("==> No compilation error files have been found [" + clog + "]");
				errLogger.println("==> No compilation error files have been found [" + clog + "]");
				System.exit(-1);
			}
			// parse for compilation error details
			CIMailSVNUtils svnUtils = new CIMailSVNUtils();
			for (String errFile: compileErrDetails.keySet()) {
				String svnInfo = svnUtils.getWCLastCommitAuthorRevisionAndMessage(prefix + "/" + errFile);
				if (svnInfo != null) {
					compileErrDetails.put(errFile, svnInfo);
				}
			}
			stdLogger.println("--------------------------------------------------");
			stdLogger.println(compileErrDetails);
			stdLogger.println("--------------------------------------------------");
		}
		catch (IOException e) {
			System.out.println("==> Oops, failed to copy/read compilation log [" + e.getMessage() + "] [CIMailParser.parse()]");
			errLogger.println("==> Oops, failed to copy/read compilation log [" + e.getMessage() + "] [CIMailParser.parse()]");
			e.printStackTrace();
			houseKeeping();
			System.exit(-1);
		}
		return true;
	}
	
	// parse() must be called before this method, otherwise, the result is undefined
	public ArrayList<String> getCompileErrLogs() {
		return compileErrLogs;
	}
	
	// parse() must be called before this method, otherwise, the result is undefined
	public HashMap<String, String> getCompileErrDetails() {
		return compileErrDetails;
	}
	
	@Override
	public void houseKeeping() {
		stdLogger.close();
		errLogger.close();
	}
	
	private PrintWriter stdLogger = stdLoggerFactory();
	private PrintWriter errLogger = errLoggerFactory();
	private ArrayList<String> compileErrLogs;
	private HashMap<String, String> compileErrDetails;
}