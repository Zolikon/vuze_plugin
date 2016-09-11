package com.zolikon.torrentmanager;

import com.zolikon.torrentmanager.scheduledservice.DownloadManagerService;
import com.zolikon.torrentmanager.scheduledservice.DownloadStartService;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.DownloadManager;

import java.util.ArrayList;
import java.util.List;


public class MainPlugin implements Plugin {

    private List<Service> services=new ArrayList<Service>();

    public void initialize(PluginInterface pluginInterface) throws PluginException {
        register(pluginInterface.getDownloadManager());
        startServices();
    }

    private void register(DownloadManager downloadManager){
        services.add(new DownloadStartService(downloadManager));
        services.add(new DownloadManagerService(downloadManager));
    }

    private void startServices(){
        for(Service service:services){
            service.startService();
        }
    }

}
