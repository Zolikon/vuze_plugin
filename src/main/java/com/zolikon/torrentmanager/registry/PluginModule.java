package com.zolikon.torrentmanager.registry;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zolikon.torrentmanager.notifier.NotificationService;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.DownloadManager;


public class PluginModule implements Module {

    private final PluginInterface pluginInterface;

    public PluginModule(PluginInterface pluginInterface) {
        this.pluginInterface = pluginInterface;
    }

    @Override
    public void configure(Binder binder) {
    }

    @Provides
    @Singleton
    public PluginConfiguration createPluginConfiguration(){
        return new PluginConfiguration();
    }

    @Provides
    @Singleton
    public DownloadManager createDownloadManager(){
        return pluginInterface.getDownloadManager();
    }

    @Provides
    @Singleton
    public NotificationService createNotificationService(PluginConfiguration configuration){
        return new NotificationService(configuration.getSlackWebhook(),configuration.getSlackUserName());
    }
}