package com.zolikon.torrentmanager.scheduledservice;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.zolikon.torrentmanager.Service;
import com.zolikon.torrentmanager.dao.TorrentDao;
import com.zolikon.torrentmanager.notifier.NotificationService;
import com.zolikon.torrentmanager.registry.PluginConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.tag.Tag;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;


@ScheduledService
public class DownloadManagerService extends AbstractScheduledService implements Service {

    private static final Logger LOG = Logger.getLogger(DownloadManagerService.class);
    private static final String SKIP_TAG = "SKIP";
    public static final String PATH_SEPARATOR = "\\";

    private final DownloadManager downloadManager;
    private final TorrentDao torrentDao;
    private final NotificationService notificationService;
    private final String copyLocation;
    private final long copyLimit;

    @Inject
    DownloadManagerService(DownloadManager downloadManager, TorrentDao torrentDao, PluginConfiguration pluginConfiguration, NotificationService notificationService) {
        this.downloadManager = downloadManager;
        this.torrentDao = torrentDao;
        this.copyLocation = pluginConfiguration.getCopyLocation();
        this.copyLimit = pluginConfiguration.getCopyLimit();
        this.notificationService = notificationService;
    }


    protected void runOneIteration() throws Exception {
        Download[] downloads = downloadManager.getDownloads();
        for (Download download : downloads) {
            if (download.isComplete()) {
                download.stopDownload();
                boolean downloadShouldBeSkipped = isMarkedSkip(download) || isUpdate(download);
                if (!downloadShouldBeSkipped) {
                    try {
                        moveDownloadedFiles(download);
                    } catch (Exception exc) {
                        LOG.error("error moving file", exc);
                    }
                } else {
                    LOG.info(String.format("Torrent named %s is marked as skip, no copy/delete will happen", download.getName()));
                }
                if (!downloadShouldBeSkipped) {
                    String message = String.format("%s download is finished", download.getName());
                    notificationService.sendNotification(message);
                }
                download.remove();
            }
        }
    }

    private boolean isMarkedSkip(Download download) {
        if(download.getName().toLowerCase().contains("vuze")){
            return true;
        }
        List<Tag> tags = download.getTags();
        boolean skip = false;
        for (Tag tag : tags) {
            if (SKIP_TAG.equals(tag.getTagName())) {
                skip = true;
            }
        }
        return skip;
    }

    private boolean isUpdate(Download download) {
        return download.getName().toLowerCase().contains("vuze");
    }

    private void moveDownloadedFiles(Download download) throws IOException {
        File targetDir = new File(getTargetDirPath(download));
        File sourceDir = new File(download.getSavePath());
        if (sourceDir.isDirectory()) {
            deleteUnwantedFilesFromDirectory(sourceDir);
            for (File item : sourceDir.listFiles()) {
                if (item.isDirectory()) {
                    FileUtils.moveDirectoryToDirectory(item, targetDir, true);
                } else {
                    FileUtils.moveFileToDirectory(item, targetDir, true);
                }
            }
            FileUtils.deleteDirectory(sourceDir);
        } else {
            FileUtils.moveFileToDirectory(sourceDir, targetDir, true);
        }
        LOG.info(String.format("Files for download %s moved to %s directory. Download removed", download.getName(), targetDir.getPath()));
    }

    private void deleteUnwantedFilesFromDirectory(File directory) throws IOException {
        File[] files = directory.listFiles(filterFilesToDelete());
        for (File item : files) {
            if (item.isDirectory()) {
                deleteUnwantedFilesFromDirectory(item);
            } else {
                FileUtils.deleteQuietly(item);
            }
        }
        if (directory.listFiles().length == 0) {
            FileUtils.deleteDirectory(directory);
        }
    }


    private String getTargetDirPath(Download download) {
        String targetDirPath = copyLocation;
        Optional<Document> optional = torrentDao.getTorrentByHash(download.getTorrentHash());
        if (optional.isPresent()) {
            LOG.debug(optional.get().get("name"));
            Document doc = optional.get();
            targetDirPath = createTargetDirPath(doc);
        } else {
            targetDirPath += PATH_SEPARATOR + download.getName() + PATH_SEPARATOR;
        }
        return targetDirPath;
    }

    private String createTargetDirPath(Document doc) {
        String targetDirPath = copyLocation;
        Object saveFolderObject = doc.get("saveFolder");
        if (saveFolderObject != null) {
            Boolean isAbsolutePath = doc.getBoolean("isAbsolutePath");
            String saveFolder = saveFolderObject.toString().replaceAll("/", "\\\\");
            if (isAbsolutePath != null && isAbsolutePath) {
                targetDirPath = saveFolder;
            } else {
                if (!saveFolder.startsWith(PATH_SEPARATOR)) {
                    saveFolder = PATH_SEPARATOR + saveFolder;
                }
                targetDirPath += saveFolder;
            }
        }
        if (!targetDirPath.endsWith(PATH_SEPARATOR)) {
            targetDirPath += PATH_SEPARATOR;
        }
        return targetDirPath;
    }

    private FileFilter filterFilesToDelete() {
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String fileName = pathname.getName().toLowerCase();
                return !((pathname.length() > copyLimit
                        || fileName.endsWith(".srt")
                        || fileName.endsWith(".sub")
                        || fileName.endsWith(".pdf")
                        || fileName.endsWith(".epub")
                        || fileName.endsWith(".mobi"))
                        && !fileName.contains("sample"));
            }
        };
    }

    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(35, 30, TimeUnit.SECONDS);
    }

    public void startService() {
        startAsync();
        LOG.info("Download manager service started");
    }

    public void stopService() {
        stopAsync();
    }
}
