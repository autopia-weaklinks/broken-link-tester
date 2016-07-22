package com.autopia;

import java.util.Properties;

import com.autopia.BasicCrawler;
import com.autopia.EmailManager;
import com.autopia.Report;
import com.autopia.Settings;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

/**
 * Main class which acts as the entry point for the broken link testing program
 * @author vj
 */
public class BrokenLinkTester {
	
	private static Properties properties = Settings.getInstance();
	
	public static void main(String args[]) throws Exception {
	    CrawlConfig config = initializeCrawlConfig();
	    startCrawlController(config);
	    
	    Report report = Report.getInstance();
	    report.closeReport();
	    
	    Boolean shouldSendEmailNotification =
	    		Boolean.parseBoolean(properties.getProperty("SendEmailNotification"));
	    if(shouldSendEmailNotification) {
	    	sendEmailNotification();
	    }
	}
	
	private static CrawlConfig initializeCrawlConfig() {
		String crawlStorageFolder = properties.getProperty("CrawlStorageFolder");
		int politenessDelay = Integer.valueOf(properties.getProperty("CrawlPolitenessDelay"));
		int maxPagesToFetch = Integer.valueOf(properties.getProperty("CrawlMaxPagesToFetch"));
		
		CrawlConfig config = new CrawlConfig();
	    config.setCrawlStorageFolder(crawlStorageFolder);
	    config.setPolitenessDelay(politenessDelay);	// Avoid sending too many requests at the same time
	    config.setMaxPagesToFetch(maxPagesToFetch);
	    config.setIncludeBinaryContentInCrawling(false);
	    config.setIncludeHttpsPages(true);
	    
	    return config;
	}
	
	private static void startCrawlController(CrawlConfig config) throws Exception {
		String baseUrl = properties.getProperty("BaseUrl");
		int crawlThreads = Integer.valueOf(properties.getProperty("CrawlThreads"));
		String crawlSeeds = properties.getProperty("CrawlSeeds");
		
		PageFetcher pageFetcher = new PageFetcher(config);
	    RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
	    RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
	    CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
	    
	    /*
	     * For each crawl, you need to add some seed urls. These are the first
	     * URLs that are fetched and then the crawler starts following links
	     * which are found in these pages
	     */
	    for(String crawlSeed : crawlSeeds.split(",")) {
	    	controller.addSeed(baseUrl + crawlSeed);
	    }
	    
	    controller.start(BasicCrawler.class, crawlThreads);	// This is a blocking operation
	}
	
	private static void sendEmailNotification() {
		String host = properties.getProperty("SmtpHost");
	    int port = Integer.parseInt(properties.getProperty("SmtpPort"));
	    String from = properties.getProperty("EmailFrom");
	    String to = properties.getProperty("EmailTo");
	    String subject = properties.getProperty("EmailSubject");
	    String body = properties.getProperty("EmailBody");
	    Boolean sslEnable = Boolean.parseBoolean(properties.getProperty("EmailSslEnable"));
	    Boolean authRequired = Boolean.parseBoolean(properties.getProperty("EmailAuthRequired"));
	    
	    EmailManager emailManager = new EmailManager(host, port);
	    emailManager.setSslEnable(sslEnable);
	    if(authRequired) {
	    	String username = properties.getProperty("EmailAuthUsername");
	    	String password = properties.getProperty("EmailAuthPassword");
	    	emailManager.setAuthentication(username, password);
	    }
	    emailManager.composeMail(from, to, subject, body);
	    emailManager.addAttachment(Report.getSuccessFile());
	    emailManager.addAttachment(Report.getFailureFile());
	    emailManager.addAttachment(Report.getCrawlerLogFile());
	    emailManager.sendMail();
	}
}