package com.zolikon.torrentmanager;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zolikon.torrentmanager.registry.PluginModule;
import com.zolikon.torrentmanager.scheduledservice.ScheduledService;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.reflections.Reflections;

import java.util.Arrays;
import java.util.Set;


public class MainPlugin implements Plugin {

    private static final Class<ScheduledService> SCHEDULED_SERVICE_CLASS = ScheduledService.class;

    public void initialize(PluginInterface pluginInterface) throws PluginException {
        Injector injector = createInjector(pluginInterface);
        Reflections reflections = new Reflections(SCHEDULED_SERVICE_CLASS.getPackage().getName());
        Set<Class<?>> servicesToStart = reflections.getTypesAnnotatedWith(SCHEDULED_SERVICE_CLASS);
        for(Class<?> serviceClass:servicesToStart){
            if(isService(serviceClass)){
                Service service = (Service) injector.getInstance(serviceClass);
                service.startService();
            }
        }
    }

    private Injector createInjector(final PluginInterface pluginInterface){
        return Guice.createInjector(new PluginModule(pluginInterface));
    }

    private boolean isService(Class<?> serviceClass) {
        Class<?>[] interfaces = serviceClass.getInterfaces();
        return Arrays.asList(interfaces).contains(Service.class);
    }

}
