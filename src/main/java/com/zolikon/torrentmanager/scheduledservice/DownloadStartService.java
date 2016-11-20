package com.zolikon.torrentmanager.scheduledservice;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.zolikon.torrentmanager.ScheduledService;
import com.zolikon.torrentmanager.Service;
import com.zolikon.torrentmanager.dao.TorrentDao;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.gudy.azureus2.plugins.download.DownloadManager;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ScheduledService
public class DownloadStartService extends AbstractScheduledService implements Service{

    private static final Logger LOG = Logger.getLogger(DownloadStartService.class);

    private final DownloadManager downloadManager;
    private final TorrentDao torrentDao;

    @Inject
    public DownloadStartService(DownloadManager downloadManager, TorrentDao torrentDao) {
        this.downloadManager = downloadManager;
        this.torrentDao = torrentDao;
    }

    protected void runOneIteration() throws Exception {
        try {
            List<Document> documentList = torrentDao.getUnprocessedDownloads();
            for (Document doc : documentList) {
                String url = doc.get("url").toString();
                downloadManager.addDownload(new URL(url), true);
                torrentDao.changeProcessedStatus(url);
                LOG.info("Download "+doc.get("name") +" added");
            }
        } catch (Exception exc) {
            LOG.error("exception during adding download",exc);
        }
    }

    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(5, 60, TimeUnit.SECONDS);
    }

    public void startService() {
        startAsync();
        LOG.info("service started");
    }

    public void stopService() {
        stopAsync();
    }
}
