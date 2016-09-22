package com.zolikon.torrentmanager.dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.zolikon.torrentmanager.MongoConfiguration;

import org.apache.log4j.Logger;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class TorrentDao {

    private static final Logger LOG = Logger.getLogger(TorrentDao.class);

    public static final String PROCESSED = "processed";
    public static final Document UPDATE_QUERY = new Document("$set", new Document(PROCESSED, true));
    public static final String URL = "url";

    private MongoConfiguration mongoConfiguration;
    private MongoCollection<Document> collection;

    @Inject
    public TorrentDao(MongoConfiguration mongoConfiguration) {
        this.mongoConfiguration = mongoConfiguration;
        connect();
    }

    private void connect() {
        try{
            MongoClient client = new MongoClient(mongoConfiguration.getMongoHost());
            this.collection = client.getDatabase(mongoConfiguration.getDatabaseName())
                    .getCollection(mongoConfiguration.getCollectionName());
            LOG.info("connected to db");
        } catch (Exception exc){
            LOG.error("database error",exc);
        }
    }

    public List<Document> getUnprocessedDownloads(){
        try{
            FindIterable<Document> iterable = collection.find(Filters.and(Filters.eq(PROCESSED, false),Filters.exists("url")));
            List<Document> result = new ArrayList<>();
            for(Document doc:iterable){
                result.add(doc);
                LOG.info("Link for " + doc.get("name") + " retrieved");
            }
            return result;
        } catch (Exception exc){
            LOG.error("database error",exc);
            connect();
            return Collections.emptyList();
        }
    }

    public void changeProcessedStatus(String url){
        try{
            collection.updateOne(Filters.eq(URL,url), UPDATE_QUERY);
        } catch (Exception exc){
            LOG.error("database error",exc);
            connect();
        }
    }

    public Optional<Document> getTorrent(String name){
        Optional<Document> result=Optional.absent();
        try{
            Document document = collection.find(Filters.eq("name", name)).first();
            if(document!=null) {
                result = Optional.of(document);
                LOG.info(result.get());
            }
        } catch(Exception exc){
            LOG.error("database error",exc);
            connect();
        }
        return result;
    }



}
