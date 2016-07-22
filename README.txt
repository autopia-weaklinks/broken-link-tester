Weak links is an open source broken link test tool that works by crawling a given web site and checking the response code for each link. It leverages the open source crawler4j API (available @ https://github.com/yasserg/crawler4j).

HOw to build the tool from source:
1. Check out the latest version from BitBucket
2. Run "mvn clean install"
3. This will generate the basic JAR file, as well as the uber JAR file with all dependencies bundled
4. This will also copy a "config.properties" file to your target folder, which is essential to set up the tool

How to test a website using the tool:
1. Update the config.properties file as required (Refer to the comments within this file for more details)
2. Execute the JAR file with dependencies from the command line, as follows:
	java -jar weaklinks-2.0-jar-with-dependencies.jar
3. Navigate to the reports folder as specified within config.properties to view the test results