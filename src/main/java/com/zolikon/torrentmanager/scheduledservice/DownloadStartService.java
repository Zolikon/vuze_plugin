package com.zolikon.torrentmanager.scheduledservice;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.sun.javafx.collections.MappingChange;
import com.zolikon.torrentmanager.MongoConfiguration;
import com.zolikon.torrentmanager.Service;
import org.bson.Document;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentCreatorImpl;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;


public class DownloadStartService extends AbstractScheduledService implements Service{

    public static final String PROCESSED = "processed";
    public static final String ID = "_id";
    private final DownloadManager downloadManager;
    private final MongoConfiguration mongoConfiguration;
    private MongoCollection<Document> collection;

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
