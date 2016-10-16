package com.zolikon.torrentmanager.dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.zolikon.torrentmanager.MongoConfiguration;

import org.apache.commons.lang3.CharSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.bouncycastle.util.Strings;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Singleton
public class TorrentDao {

    private static final Logger LOG = Logger.getLogger(TorrentDao.class);

    private static final String PROCESSED = "processed";
    private static final Document UPDATE_QUERY = new Document("$set", new Document(PROCESSED, true));
    private static final String URL = "url";

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

    @Deprecated
    public Optional<Document> getTorrentByName(String name){
        return getTorrent(Filters.eq("name", name));
    }

    public Optional<Document> getTorrentByHash(byte[] hash){
        LOG.debug(bytesToHex(hash));
        return getTorrent(new Document("$text", new Document("$search",bytesToHex(hash))));
    }

    private String bytesToHex(byte[] hash){
        StringBuilder builder = new StringBuilder();
        for(byte b:hash){
            builder.append(String.format("%02x",b));
        }
        return builder.toString();
    }

    private Optional<Document> getTorrent(Bson query){
        Optional<Document> result=Optional.absent();
        try{
            Document document = collection.find(query).first();
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
