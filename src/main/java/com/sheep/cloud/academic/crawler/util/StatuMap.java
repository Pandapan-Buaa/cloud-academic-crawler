package com.sheep.cloud.academic.crawler.util;

import java.util.concurrent.ConcurrentHashMap;

public class StatuMap {
    private static StatuMap instance = new StatuMap();
    private ConcurrentHashMap<String, InnerStatu> statumap = new ConcurrentHashMap<>();

    public static StatuMap getInstance(){
        return instance;
    }
    public ConcurrentHashMap<String,InnerStatu> getStatumap(){
        return statumap;
    }
    private StatuMap(){}

}
