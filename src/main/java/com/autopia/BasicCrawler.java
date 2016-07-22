package com.autopia;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import com.autopia.Report;
import com.autopia.Settings;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

/**
 * Implementation of the {@link WebCrawler} interface.
 * This class contains the logic required to crawl through the site being tested for broken links.
 * @author vj
 */
public class BasicCrawler extends WebCrawler {
	
	private static final Properties PROPERTIES = Settings.getInstance();
	
	private static final String BASE_URL = PROPERTIES.getProperty("BaseUrl").replaceAll("https?://", "");
	
	private static final Pattern IMAGE_EXTENSIONS =
				Pattern.compile(PROPERTIES.getProperty("ImageExtensionsPattern"));
	
	private static final Boolean SHOULD_NOT_VISIT_SPECIFIED_URLS =
			Boolean.parseBoolean(PROPERTIES.getProperty("DontVisitSpecifiedUrls"));
	private static final Pattern DONT_VISIT_URLS =
			Pattern.compile(PROPERTIES.getProperty("DontVisitUrlsPattern"));
	
	private static final Boolean FLAG_SPECIFIED_ERROR_PAGES =
			Boolean.parseBoolean(PROPERTIES.getProperty("FlagSpecifiedErrorPages"));
	private static final Pattern ERROR_PAGE_URLS =
			Pattern.compile(PROPERTIES.getProperty("ErrorPageUrlsPattern"));
	
	private static final Boolean PROXY_REQUIRED =
			Boolean.parseBoolean(PROPERTIES.getProperty("ProxyRequired"));
	private static final String PROXY_HOST = PROPERTIES.getProperty("ProxyHost");
	private static final int PROXY_PORT =
							Integer.parseInt(PROPERTIES.getProperty("ProxyPort"));
	
	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String pageUrl = url.getURL().toLowerCase();
		
		// Ignore the url if it has an extension that matches our defined set of image extensions
		if (IMAGE_EXTENSIONS.matcher(pageUrl).matches()) {
			return false;
		}
		
		// Ignore the url if it contains any of the patterns mentioned within our DONT_VISIT_URLs setting
		if (SHOULD_NOT_VISIT_SPECIFIED_URLS &&
				DONT_VISIT_URLS.matcher(pageUrl).matches()) {
			return false;
		}
		
		// Only accept the url if it is in the given BASE URL domain
		return pageUrl.startsWith("https://" + BASE_URL) ||
				pageUrl.startsWith("http://" + BASE_URL);
	}
	
	@Override
	public void visit(Page page) {
		String pageUrl = page.getWebURL().getURL();
		String linkUrl, linkAnchor;
		
		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			Set<WebURL> links = htmlParseData.getOutgoingUrls();
			
			logger.info("Visiting page: " + pageUrl + ", Outgoing links: " + links.size());
			
			for(WebURL link : links) {
				linkAnchor = link.getAnchor();
				if(linkAnchor == null) {
					linkAnchor = "N/A";
				} else {
					linkAnchor = linkAnchor.replace(",", ";");
				}
				
				linkUrl = link.getURL();
				reportLinkStatus(pageUrl, linkAnchor, linkUrl);
			}
		}
	}
	
	private void reportLinkStatus(String referrer, String anchor, String url) {
		HttpURLConnection connection = getConnection(url);
		if (null == connection) {
			logger.error("Null connection - unable to report link status!");
			return;
		}
		
		int responseCode = getResponseCode(connection);
		int responseCodeClass = responseCode / 100;
		String line = "\"" + referrer + "\",\"" + anchor + "\",\"" + url + "\"," + responseCode + ",";
		Report report = Report.getInstance();
		
		switch(responseCodeClass) {
			case 2:
				line += "OK";
				connection.disconnect();
				report.updateReport(line, true);
				break;
				
			case 3:
				String finalUrl = getFinalUrlAfterRedirection(connection);
				
				// Check for redirects to any of the ERROR_PAGE_URLS
				if(FLAG_SPECIFIED_ERROR_PAGES &&
						ERROR_PAGE_URLS.matcher(finalUrl).matches()) {
					line += "Redirect - Error Page";
					report.updateReport(line, false);
				} else {
					line += "Redirect - OK";
					report.updateReport(line, true);
				}
				//connection.disconnect();
				
				break;
				
			case 4:
				line += "Client error";
				connection.disconnect();
				report.updateReport(line, false);
				break;
				
			case 5:
				line += "Server error";
				connection.disconnect();
				report.updateReport(line, false);
				break;
				
			default:
				line += "Unknown";
				connection.disconnect();
				report.updateReport(line, false);
		}
	}
	
  	private HttpURLConnection getConnection(String urlString) {
		try {
			HttpURLConnection connection;
			
			if(PROXY_REQUIRED) {
				InetSocketAddress address = new InetSocketAddress(PROXY_HOST, PROXY_PORT);
				Proxy proxy = new Proxy(Type.HTTP, address);
				connection = (HttpURLConnection) new URL(urlString).openConnection(proxy);
			} else {
				connection = (HttpURLConnection) new URL(urlString).openConnection();
			}
			
			connection.setInstanceFollowRedirects(false);
			//connection.setConnectTimeout(5000);
			//connection.setReadTimeout(5000);
			connection.connect();
			return connection;
		} catch (Exception ex) {
			logger.error("Error while connecting to " + urlString + ": " + ex.getMessage(), ex);
			return null;
		}
	}
  	
  	private int getResponseCode(HttpURLConnection connection) {
		try {
			return connection.getResponseCode();
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return 0;	// Unknown
		}
	}
	
	private String getFinalUrlAfterRedirection(HttpURLConnection connection) {
		String Url = connection.getURL().toString();
		String finalUrl = Url;
	    String redirectedUrl;
		
		while (true) {
			redirectedUrl = connection.getHeaderField("Location");
			if (null == redirectedUrl) {
				return finalUrl;
			}
			
			logger.info(finalUrl + " redirected to " + redirectedUrl);
			// TODO: Include this info directly within the report?
			
			if(!redirectedUrl.contains("http")) {
				redirectedUrl = handleRedirectsWithoutProtocol(finalUrl, redirectedUrl);
			}
			
			connection = getConnection(redirectedUrl);
			finalUrl = redirectedUrl;
			if (null == connection) {
		    	return finalUrl;
		    }
			
			if (getResponseCode(connection) / 100 == 2) {
				break;
			}
		}
		
		connection.disconnect();
		return finalUrl;
	}
	
	private String handleRedirectsWithoutProtocol(String baseUrl, String redirectedUrl) {
		if(baseUrl.contains("?")) {
			// Handle redirects such as 'press_kits.cfm?presskit_id=68'
			// from base URL of 'http://m.news.prudential.com/press_file.cfm?presskit_id=68'
			int position = baseUrl.lastIndexOf("/");
			return baseUrl.substring(0, position + 1) + redirectedUrl;
		} else {
			// Handle redirects such as '/home'
			return baseUrl + redirectedUrl;
		}
	}
}