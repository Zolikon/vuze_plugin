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

    private static final int COPY_LIMIT = 1024 * 1024 * 20;
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
                    LOG.error("error moving file",exc);
                }
                download.remove();
            }
        }
    }

    private void moveDownloadedFiles(Download download) throws IOException {
        File targetDir = new File(getTargetDirPath(download));
        File sourceDir =new File(download.getSavePath());
        if(sourceDir.isDirectory()){
            deleteUnwantedFilesFromDirectory(sourceDir);
            for(File item:sourceDir.listFiles()){
                if(item.isDirectory()){
                    FileUtils.moveDirectoryToDirectory(item,targetDir,true);
                } else {
                    FileUtils.moveFileToDirectory(item,targetDir,true);
                }
            }
            FileUtils.deleteDirectory(sourceDir);
        } else {
            FileUtils.moveFileToDirectory(sourceDir,targetDir,true);
        }
        LOG.info(String.format("Files for download %s moved to %s directory. Download removed",download.getName(),targetDir.getPath()));
    }

    private void deleteUnwantedFilesFromDirectory(File directory) throws IOException {
        File[] files = directory.listFiles(filterFilesToDelete());
        for(File item:files){
            if(item.isDirectory()){
                deleteUnwantedFilesFromDirectory(item);
            } else {
                FileUtils.deleteQuietly(item);
            }
        }
        if(directory.listFiles().length==0){
            FileUtils.deleteDirectory(directory);
        }
    }


    private String getTargetDirPath(Download download) {
        String targetDirPath=COPY_LOCATION;
        Optional<Document> optional = torrentDao.getTorrentByHash(download.getTorrentHash());
        if(optional.isPresent()){
            LOG.debug(optional.get().get("name"));
            Document doc = optional.get();
            targetDirPath = createTargetDirPath(doc);
        } else {
            targetDirPath+="\\"+download.getName()+"\\";
        }
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
                if(!saveFolder.startsWith("\\")){
                    saveFolder="\\"+saveFolder;
                }
                targetDirPath+=saveFolder;
            }
        }
        if(!targetDirPath.endsWith("\\")){
            targetDirPath+="\\";
        }
        return targetDirPath;
    }

    private FileFilter filterFilesToDelete() {
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String fileName = pathname.getName();
                return !((pathname.length()> COPY_LIMIT
                        || fileName.contains(".srt")
                        || fileName.contains(".sub")
                        || fileName.contains(".pdf")
                        || fileName.contains(".epub")
                        || fileName.contains(".mobi"))
                        && !fileName.contains("sample"));
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
