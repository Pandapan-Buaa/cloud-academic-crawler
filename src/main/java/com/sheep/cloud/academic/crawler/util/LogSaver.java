package com.sheep.cloud.academic.crawler.util;


import org.springframework.stereotype.Component;

import java.util.ArrayList;

public class LogSaver {

    private ArrayList<String> saver = new ArrayList<String>();
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

    public ArrayList<String> getSaver(){
        return saver;
    }
}
