package com.zolikon.torrentmanager;

import com.google.inject.Singleton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Singleton
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ScheduledService {
}
