package com.autopia;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import uk.org.lidalia.slf4jext.Logger;
import uk.org.lidalia.slf4jext.LoggerFactory;


/**
 * Singleton class that encapsulates the user settings specified in the properties file of the framework
 * @author vj
 */
public class Settings {
	private static final Logger logger = LoggerFactory.getLogger(Settings.class);
	private static Properties properties = loadFromPropertiesFile();
	
	private Settings() {
		// To prevent external instantiation of this class
	}
	
	/**
	 * Function to return the singleton instance of the {@link Properties} object
	 * @return Instance of the {@link Properties} object
	 */
	public static Properties getInstance() {
		return properties;
	}
	
	private static Properties loadFromPropertiesFile() {
		Properties properties = new Properties();
		
		try {
			File externalSettingsFile = new File("./config.properties");
			
			if(externalSettingsFile.exists()) {
				logger.info("Loading settings from external properties file");
				properties.load(new FileInputStream(externalSettingsFile));
			} else {
				logger.info("Loading settings from internal properties file");
				InputStream internalSettingsFileStream =
						Settings.class.getClassLoader().getResourceAsStream("config.properties");
				properties.load(internalSettingsFileStream);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		
		return properties;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
}