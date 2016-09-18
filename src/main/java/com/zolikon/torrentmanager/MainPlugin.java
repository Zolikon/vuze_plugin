package com.zolikon.torrentmanager;

import com.google.inject.*;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.reflections.Reflections;

import java.util.Set;


public class MainPlugin implements Plugin {

    public void initialize(PluginInterface pluginInterface) throws PluginException {
        Injector injector = createInjector(pluginInterface);
        Reflections reflections = new Reflections(ScheduledService.class.getPackage().getName());
        Set<Class<?>> servicesToStart = reflections.getTypesAnnotatedWith(ScheduledService.class);
        for(Class<?> serviceClass:servicesToStart){
            if(isService(serviceClass)){
                Service service = (Service) injector.getInstance(serviceClass);
                service.startService();
            }
        }
    }

    private Injector createInjector(final PluginInterface pluginInterface){
        class ProductionModule implements Module {
            @Provides
            @Singleton
            public DownloadManager createDownloadManager(){
                return pluginInterface.getDownloadManager();
            }

            @Override
            public void configure(Binder binder) {

            }
        }
        return Guice.createInjector(new ProductionModule());
    }

    private boolean isService(Class<?> serviceClass) {
        Class<?>[] interfaces = serviceClass.getInterfaces();
        boolean matchFound = false;
        for(Class<?> item:interfaces){
            if(item.equals(Service.class)){
                matchFound = true;
                break;
            }
        }
        return matchFound;
    }

}
