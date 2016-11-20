package com.zolikon.torrentmanager;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class PluginConfiguration {

    private static final Logger LOG = Logger.getLogger(PluginConfiguration.class);

    private String mongoHost;
    private String databaseName;
    private String collectionName;

    private long copyLimit;
    private String copyLocation;


    public PluginConfiguration() {
        Properties properties = new Properties();
        try {
            ClassLoader classLoader = this.getClass().getClassLoader();
            InputStream resourceAsStream = classLoader.getResourceAsStream("configuration.properties");
            properties.load(resourceAsStream);
        } catch (IOException e) {
            LOG.warn("Property file not found, using default values.");
        }
        mongoHost = properties.getProperty("mongoHost", "localhost:27017");
        databaseName = properties.getProperty("databaseName", "test");
        collectionName = properties.getProperty("collectionName", "test");
        String limitInMegaByte = properties.getProperty("copyLimit", "20");
        try {
            copyLimit = Integer.parseInt(limitInMegaByte) * 1024 * 1024;
        } catch (NumberFormatException exc) {
            copyLimit = 20 * 1024 * 1024;
        }
        copyLocation = properties.getProperty("defaultCopyLocation", "d:\\New");
    }

    public String getMongoHost() {
        return mongoHost;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public long getCopyLimit() {
        return copyLimit;
    }

    public String getCopyLocation() {
        return copyLocation;
    }
}
