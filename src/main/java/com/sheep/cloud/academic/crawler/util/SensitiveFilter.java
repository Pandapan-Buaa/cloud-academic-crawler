package com.sheep.cloud.academic.crawler.util;


import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    //初始化前缀树
    private static final String REPLACEMENT = "***";
    private TrieNode rootNode = new TrieNode();

    @PostConstruct
    public void init() {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                //添加到前缀树
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败" + e.getMessage());
        }
    }

    //将敏感词添加到前缀树中
    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            if (subNode == null) {
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);

            }
            tempNode = subNode;
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }

    }

    /*
     * 过滤敏感词
     * @param  text 待过滤文本
     * @return 过滤后文本
     * */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        TrieNode tempNode = rootNode;
        int begin = 0;
        int position = 0;
        StringBuilder res = new StringBuilder();

        while (position < text.length()) {
            char c = text.charAt(position);
            //跳过符号
            if (isSymbol(c)){
                //指针1处于根节点，符号计入结果，指针2向下走一步
                if (tempNode == rootNode){
                    res.append(c);
                    begin++;
                }
                //无论符号在开头还是中间，指针3都向下走一步
                position++;
                continue;
            }
            tempNode =  tempNode.getSubNode(c);
            if(tempNode == null){
                //以begin开头字符非敏感词
                res.append(text.charAt(begin));
                position = ++begin;
                tempNode = rootNode;
            }
            else if(tempNode.isKeywordEnd){
                res.append(REPLACEMENT);
                begin = ++position;
                tempNode = rootNode;
            }else{
                position++;
            }

        }
        res.append(text.substring(begin));
        return res.toString();
    }

    public boolean find(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        TrieNode tempNode = rootNode;
        int begin = 0;
        int position = 0;

        while (position < text.length()) {
            char c = text.charAt(position);
            //跳过符号
            if (isSymbol(c)){
                //指针1处于根节点，符号计入结果，指针2向下走一步
                if (tempNode == rootNode){
//                    res.append(c);
                    begin++;
                }
                //无论符号在开头还是中间，指针3都向下走一步
                position++;
                continue;
            }
            tempNode =  tempNode.getSubNode(c);
            if(tempNode == null){
                //以begin开头字符非敏感词
//                res.append(text.charAt(begin));
                position = ++begin;
                tempNode = rootNode;
            }
            else if(tempNode.isKeywordEnd){
//                res.append(REPLACEMENT);
                begin = ++position;
                tempNode = rootNode;
                return true;
            }else{
                position++;
            }

        }
//        res.append(text.substring(begin));
        return false;
    }

    private boolean isSymbol(Character c) {
        //0x2E80~0X9FFF是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9fff);
    }

    //前缀树
    private class TrieNode {
        //关键词结束标志
        private boolean isKeywordEnd = false;
        //子节点<key是下级字符，value是下级节点>
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }
    }


}
