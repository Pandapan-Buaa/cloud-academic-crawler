package com.sheep.cloud.academic.crawler.util;

import com.sheep.cloud.academic.crawler.entity.ScholarTemp;

import java.util.*;


public class InnerStatu{
    public List<String> saver;
    public List<String> errsaver;
    public List<String> errlog;
    public Set<String> set;
    public List<ScholarTemp> scholars;
    public long antiCrawlerStatus = 0;
    public long antiCrawlerSize = 0;
    public long detailMatchStatus = 0;
    public long detailMatchSize = 0;
    public long detailStatus = 0;
    public long detailSize = 0;
    public long imgCrawlerStatus = 0;
    public long imgCrawlerSize = 0;
    public long crawlerStatus = 0;
    public long crawlerSize = 0;
    public long loadConfigStatus = 0;
    public long loadConfigSize = 0;
    public InnerStatu(){
        saver = Collections.synchronizedList(new ArrayList<>());
        errsaver = Collections.synchronizedList(new ArrayList<>());
        errlog = Collections.synchronizedList(new ArrayList<>());
        set = new HashSet<>();
        scholars = Collections.synchronizedList(new ArrayList<>());
    }
    public void logclear(){
        saver.clear();
        errsaver.clear();
    }
    public void clear(){
        saver.clear();
        errsaver.clear();
        antiCrawlerStatus = 0;
        antiCrawlerSize = 0;
        detailMatchStatus = 0;
        detailMatchSize = 0;
        detailStatus = 0;
        detailSize = 0;
        imgCrawlerStatus = 0;
        imgCrawlerSize = 0;
        crawlerStatus = 0;
        crawlerSize = 0;
        loadConfigStatus = 0;
        loadConfigSize = 0;
    }
}
