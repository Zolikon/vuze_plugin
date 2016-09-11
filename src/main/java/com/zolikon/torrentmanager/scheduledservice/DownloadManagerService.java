package com.zolikon.torrentmanager.scheduledservice;

import com.google.common.util.concurrent.AbstractScheduledService;

import com.zolikon.torrentmanager.Service;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;

import java.util.concurrent.TimeUnit;

public class DownloadManagerService extends AbstractScheduledService implements Service {

    private final DownloadManager downloadManager;

    public DownloadManagerService(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    protected void runOneIteration() throws Exception {
        Download[] downloads = downloadManager.getDownloads();
        for(Download download:downloads){
            if(download.isComplete()){
                download.stopDownload();
                download.remove();
            }
        }
    }

    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(35, 300, TimeUnit.SECONDS);
    }

    public void startService() {
        startAsync();
    }

    public void stopService() {
        stopAsync();
    }
}
