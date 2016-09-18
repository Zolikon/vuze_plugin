package com.zolikon.torrentmanager.scheduledservice;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.zolikon.torrentmanager.ScheduledService;
import com.zolikon.torrentmanager.Service;
import org.apache.commons.io.FileUtils;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


@ScheduledService
public class DownloadManagerService extends AbstractScheduledService implements Service {

    public static final int COPY_LIMIT = 1024 * 1024 * 50;
    private static final String COPY_LOCATION = "d:\\New\\";
    private final DownloadManager downloadManager;


    @Inject
    public DownloadManagerService(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    protected void runOneIteration() throws Exception {
        Download[] downloads = downloadManager.getDownloads();
        File targetDir=new File(COPY_LOCATION);
        for(Download download:downloads){
            if(download.isComplete()){
                download.stopDownload();
                moveDownloadedFiles(targetDir, download);
                download.remove();
            }
        }
    }

    private void moveDownloadedFiles(File targetDir, Download download) throws IOException {
        File sourceDir =new File(download.getSavePath());
        if(sourceDir.isDirectory()){
            File[] files = sourceDir.listFiles(filterFilesToMove());
            for(File item:files){
                FileUtils.moveFileToDirectory(item,targetDir,true);
            }
            FileUtils.deleteDirectory(sourceDir);
        } else {
            FileUtils.moveFileToDirectory(sourceDir,targetDir,true);
        }
    }

    private FileFilter filterFilesToMove() {
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String fileName = pathname.getName();
                return pathname.isDirectory()
                        || pathname.length()> COPY_LIMIT
                        || fileName.contains(".srt")
                        || fileName.contains(".sub")
                        || fileName.contains(".pdf")
                        || fileName.contains(".epub")
                        || fileName.contains(".mobi");
            }
        };
    }

    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(35, 30, TimeUnit.SECONDS);
    }

    public void startService() {
        startAsync();
    }

    public void stopService() {
        stopAsync();
    }
}
