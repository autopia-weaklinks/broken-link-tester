package com.autopia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import com.autopia.Settings;
import com.autopia.Util;

import uk.org.lidalia.slf4jext.Logger;
import uk.org.lidalia.slf4jext.LoggerFactory;

/**
 * The reporting class for the broken link test program.
 * This is a singleton class, to allow for report consolidation from multiple threads.
 * @author vj
 */
public class Report {
	
	private static final Logger logger = LoggerFactory.getLogger(Report.class);
	private static final Properties PROPERTIES = Settings.getInstance();
	private static final String REPORT_PATH = PROPERTIES.getProperty("ReportPath");
	
	private static String reportPathWithTimeStamp;
	private static File successFile;
	private static File failureFile;
	private static File crawlerLogFile;
	private static FileWriter successFileWriter;
	private static FileWriter failureFileWriter;
	private static BufferedWriter successBufferedWriter;
	private static BufferedWriter failureBufferedWriter;
	
	private static final Report REPORT = new Report();
	
	// To prevent external instantiation of this class
	private Report() {
		String timeStamp = "Run_" +
							Util.getCurrentFormattedTime("dd-MMM-yyyy hh:mm:ss a")
							.replace(" ", "_").replace(":", "-");
		
		reportPathWithTimeStamp =
							REPORT_PATH + Util.getFileSeparator() + timeStamp;
		
		new File(reportPathWithTimeStamp).mkdirs();
		successFile = new File(reportPathWithTimeStamp +
							Util.getFileSeparator() + "SiteLinks_SuccessReport.csv");
		failureFile = new File(reportPathWithTimeStamp +
							Util.getFileSeparator() + "SiteLinks_FailureReport.csv");
		crawlerLogFile = new File(System.getProperty("user.dir") +
							Util.getFileSeparator() +  "logs" + 
							Util.getFileSeparator() + "crawler4j.log");
		
		try {
			successFile.createNewFile();
			failureFile.createNewFile();
			successFileWriter = new FileWriter(successFile.getAbsoluteFile());
			failureFileWriter = new FileWriter(failureFile.getAbsoluteFile());
			successBufferedWriter = new BufferedWriter(successFileWriter);
			failureBufferedWriter = new BufferedWriter(failureFileWriter);
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}
		
		String headers = "Referrer,Anchor,FinalURL,ResponseCode,Status";
		updateReport(headers, true);
		updateReport(headers, false);
		
		logger.info("Report initialized successfully");
	}
	
	/**
	 * Function to return the Singleton instance of the {@link Report} class
	 * @return Instance of the {@link Report} class 
	 */
	public static Report getInstance() {
		return REPORT;
	}
	
	/**
	 * Function to get the success report file
	 * @return The success report file
	 */
	public static File getSuccessFile() {
		return successFile;
	}
	
	/**
	 * Function to get the failure report file
	 * @return The failure report file
	 */
	public static File getFailureFile() {
		return failureFile;
	}
	
	/**
	 * Function to get the crawler log file
	 * @return The crawler log file
	 */
	public static File getCrawlerLogFile() {
		return crawlerLogFile;
	}
	
	/**
	 * Function to update the test report as per the user input
	 * @param line A comma separated string representing one line within the CSV report
	 * @param successMessage Boolean value indicating whether the current message indicates a success or a failure
	 */
	public synchronized void updateReport(String line, Boolean successMessage) {
		try {
			if(successMessage) {
				successBufferedWriter.write(line);
				successBufferedWriter.newLine();
			} else {
				failureBufferedWriter.write(line);
				failureBufferedWriter.newLine();
			}
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}
	
	/**
	 * Function to close the report
	 */
	public void closeReport() {
		try {
			successBufferedWriter.close();
			failureBufferedWriter.close();
			
			logger.info("Report closed successfully");
			
			File crawlerLogFileCopy = new File(reportPathWithTimeStamp +
											Util.getFileSeparator() + "crawler4j.log");
			
			Files.copy(crawlerLogFile.toPath(), crawlerLogFileCopy.toPath());
			crawlerLogFile = crawlerLogFileCopy;
			
			logger.info("Log file copied successfully");
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
}