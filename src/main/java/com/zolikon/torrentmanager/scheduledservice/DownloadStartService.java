package com.zolikon.torrentmanager.scheduledservice;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.zolikon.torrentmanager.MongoConfiguration;
import com.zolikon.torrentmanager.ScheduledService;
import com.zolikon.torrentmanager.Service;
import org.bson.Document;
import org.gudy.azureus2.plugins.download.DownloadManager;

import java.net.URL;
import java.util.concurrent.TimeUnit;

@ScheduledService
public class DownloadStartService extends AbstractScheduledService implements Service{

    public static final String PROCESSED = "processed";
    public static final String ID = "_id";
    private final DownloadManager downloadManager;
    private final MongoConfiguration mongoConfiguration;
    private MongoCollection<Document> collection;

    @Inject
    public DownloadStartService(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
        this.mongoConfiguration = new MongoConfiguration();
        try {
            connectToDb();
        } catch (Exception exc) {

        }
    }

    protected void runOneIteration() throws Exception {
        if (collection == null) {
            connectToDb();
        }
        try {
            FindIterable<Document> iterable = collection.find(Filters.eq(PROCESSED, false));
            for (Document doc : iterable) {
                String url = doc.getString("url");
                Object id = doc.get(ID);
                downloadManager.addDownload(new URL(url), true);
                doc.append(PROCESSED, true);
                collection.replaceOne(Filters.eq(ID, id), doc);
            }
        } catch (Exception exc) {
            collection = null;
        }
    }

    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(5, 60, TimeUnit.SECONDS);
    }

    private void connectToDb() {

        MongoClient client = new MongoClient(mongoConfiguration.getMongoHost());
        collection = client.getDatabase(mongoConfiguration.getDatabaseName()).getCollection(mongoConfiguration.getCollectionName());
    }

    public void startService() {
        startAsync();
    }

    public void stopService() {
        stopAsync();
    }
}
