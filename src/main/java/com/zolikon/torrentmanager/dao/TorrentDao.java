package com.zolikon.torrentmanager.dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.zolikon.torrentmanager.PluginConfiguration;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Singleton
public class TorrentDao {

    private static final Logger LOG = Logger.getLogger(TorrentDao.class);

    private static final String PROCESSED = "processed";
    private static final String URL = "url";
    private static final String PROCESSED_DATE_TIME = "processedDateTime";

    private PluginConfiguration mongoConfiguration;
    private MongoCollection<Document> collection;

    @Inject
    public TorrentDao(PluginConfiguration mongoConfiguration) {
        this.mongoConfiguration = mongoConfiguration;
        connect();
    }

    private void connect() {
        try {
            MongoClient client = new MongoClient(mongoConfiguration.getMongoHost());
            this.collection = client.getDatabase(mongoConfiguration.getDatabaseName())
                    .getCollection(mongoConfiguration.getCollectionName());
            LOG.info("connected to db");
        } catch (Exception exc) {
            LOG.error("database error", exc);
        }
    }

    public List<Document> getUnprocessedDownloads() {
        try {
            FindIterable<Document> iterable = collection.find(Filters.and(Filters.eq(PROCESSED, false), Filters.exists(URL)));
            List<Document> result = new ArrayList<>();
            for (Document doc : iterable) {
                result.add(doc);
                LOG.info("Link for " + doc.get("name") + " retrieved");
            }
            return result;
        } catch (Exception exc) {
            LOG.error("database error", exc);
            connect();
            return Collections.emptyList();
        }
    }

    public void changeProcessedStatus(String url) {
        try {
            collection.updateOne(Filters.eq(URL, url), createUpdateQuery());
        } catch (Exception exc) {
            LOG.error("database error", exc);
            connect();
        }
    }

    private Document createUpdateQuery() {
        return new Document("$set", new Document(PROCESSED, true).append(PROCESSED_DATE_TIME, new Date()));
    }

    public Optional<Document> getTorrentByHash(byte[] hash) {
        LOG.debug(bytesToHex(hash));
        return getTorrent(new Document("$text", new Document("$search", bytesToHex(hash))));
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder builder = new StringBuilder();
        for (byte b : hash) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private Optional<Document> getTorrent(Bson query) {
        Optional<Document> result = Optional.absent();
        try {
            Document document = collection.find(query).first();
            if (document != null) {
                result = Optional.of(document);
                LOG.info(document);
            }
        } catch (Exception exc) {
            LOG.error("database error", exc);
            connect();
        }
        return result;
    }


}
