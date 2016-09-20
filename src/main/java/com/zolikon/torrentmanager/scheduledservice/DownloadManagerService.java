package com.zolikon.torrentmanager.scheduledservice;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.zolikon.torrentmanager.ScheduledService;
import com.zolikon.torrentmanager.Service;
import com.zolikon.torrentmanager.dao.TorrentDao;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


@ScheduledService
public class DownloadManagerService extends AbstractScheduledService implements Service {

    private static final Logger LOG = Logger.getLogger(DownloadManagerService.class);

    public static final int COPY_LIMIT = 1024 * 1024 * 50;
    private static final String COPY_LOCATION = "d:\\New";
    private final DownloadManager downloadManager;
    private final TorrentDao torrentDao;


    @Inject
    public DownloadManagerService(DownloadManager downloadManager, TorrentDao torrentDao) {
        this.downloadManager = downloadManager;
        this.torrentDao = torrentDao;
    }


    protected void runOneIteration() throws Exception {
        Download[] downloads = downloadManager.getDownloads();
        for(Download download:downloads){
            if(download.isComplete()){
                download.stopDownload();
                try{
                    moveDownloadedFiles(download);
                } catch (Exception exc){
                    LOG.error("error moving file");
                }
                download.remove();
            }
        }
    }

    private void moveDownloadedFiles(Download download) throws IOException {
        File targetDir = new File(getTargetDirPath(download));
        LOG.debug(targetDir.getPath());
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
        LOG.info(String.format("Files for download %s moved to % directory. Download removed",download.getName(),targetDir.getPath()));
    }

    private String getTargetDirPath(Download download) {
        String targetDirPath=COPY_LOCATION;
        Optional<Document> optional = torrentDao.getTorrent(download.getName());
        if(optional.isPresent()){
            Document doc = optional.get();
            targetDirPath = createTargetDirPath(doc);
        }
        LOG.debug(targetDirPath);
        return targetDirPath;
    }

    private String createTargetDirPath(Document doc) {
        String targetDirPath = COPY_LOCATION;
        Object saveFolderObject = doc.get("saveFolder");
        if(saveFolderObject!=null){
            Boolean isAbsolutePath = doc.getBoolean("isAbsolutePath");
            String saveFolder = saveFolderObject.toString().replaceAll("/","\\\\");
            if(isAbsolutePath !=null&& isAbsolutePath){
                targetDirPath=saveFolder;
            } else {
                targetDirPath+=saveFolder;
            }
        }
        if(!targetDirPath.endsWith("\\")){
            targetDirPath+="\\";
        }
        return targetDirPath;
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
        LOG.info("service started");
    }

    public void stopService() {
        stopAsync();
    }
}
