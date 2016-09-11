package com.zolikon.torrentmanager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class MongoConfiguration {

    private String mongoHost;
    private String databaseName;
    private String collectionName;


    public MongoConfiguration() {
        Properties properties = new Properties();
        try {
            ClassLoader classLoader = this.getClass().getClassLoader();
            InputStream resourceAsStream = classLoader.getResourceAsStream("configuration.properties");
            properties.load(resourceAsStream);
        } catch (IOException e) {

        }
        mongoHost = properties.getProperty("mongoHost", "localhost:27017");
        databaseName = properties.getProperty("databaseName", "test");
        collectionName = properties.getProperty("collectionName", "test");
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
}
