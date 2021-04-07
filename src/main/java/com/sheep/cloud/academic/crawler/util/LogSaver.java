package com.sheep.cloud.academic.crawler.util;


import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogSaver {

    private List<String> saver = Collections.synchronizedList(new ArrayList<>());
    private static LogSaver instance = new LogSaver();
    private LogSaver(){}

    public static LogSaver getInstance(){
        return instance;
    }

    public void clean(){
        saver.clear();
    }

    public void add(String str){
        saver.add(str);
    }

    public List<String> getSaver(){
        return saver;
    }
}
