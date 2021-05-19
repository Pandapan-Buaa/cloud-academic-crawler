package com.sheep.cloud.academic.crawler.webmagic;

import com.sheep.cloud.academic.crawler.entity.ScholarConfigure;
import com.sheep.cloud.academic.crawler.entity.ScholarConfigureTemp;
import com.sheep.cloud.academic.crawler.entity.ScholarTemp;
import com.sheep.cloud.academic.crawler.util.CrawlerUtil;

import com.sheep.cloud.academic.crawler.util.StatuMap;
import com.sheep.cloud.core.util.CollectionUtil;
import com.sheep.cloud.core.util.StringUtil;
import com.sheep.cloud.open.mongodb.util.MongodbUtil;
import lombok.extern.slf4j.Slf4j;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import static com.sheep.cloud.academic.crawler.controller.ScholarTempController.ORG_NAME_URLS;
import static com.sheep.cloud.academic.crawler.webmagic.ScholarSpider.PATTERN;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @author Fan zk
 */

@Slf4j
public class ScholarImgSpider implements PageProcessor {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setTimeOut(30000);

    private ScholarConfigure configure;

    private String charset;
    private String namehash;
//    LogSaver logSaver = LogSaver.getInstance();

    public ScholarImgSpider(ScholarConfigure configure, String charset,String namehash) {
        this.configure = configure;
        this.charset = charset;
        this.namehash  = namehash;
    }

    @Override
    public void process(Page page) {
        Html html = page.getHtml();
        String organizationName = configure.getOrganizationName();
        String collegeName = configure.getCollegeName();
        String dept = StringUtil.isEmpty(configure.getDepartmentName()) ? "" : configure.getDepartmentName();
        String title = StringUtil.isEmpty(configure.getTitle()) ? "" : configure.getTitle();

        Selectable selectable = html.xpath(configure.getXpath());
        List<Selectable> nodes = selectable.xpath("//a").nodes();

        List<ScholarTemp> scholars = new ArrayList<>();
        String nodeStr = CrawlerUtil.getTextTrim(selectable);

        for (Selectable node : nodes) {
            String name = CrawlerUtil.getTextTrim(node);
            if (StringUtil.isBlank(name)) {
                name = CrawlerUtil.replace(node.xpath("//a/@title").toString());
            }
            if (StringUtil.isBlank(name)) {
                name = CrawlerUtil.replace(node.xpath("//a/@textvalue").toString());
            }
            if (StringUtil.isBlank(name)) {
                if (node.xpath("//img").nodes().isEmpty()) {
                    continue;
                }
                ScholarTemp scholar = parseNode(node, organizationName, collegeName, dept, title, nodeStr);
                if (null != scholar) {
                    if (PATTERN.matcher(scholar.getName()).find()) {
                        continue;
                    }
                    scholar.setContent(CrawlerUtil.simplifyContent(scholar.getContent(), scholar));
                    String code = organizationName + "|" + collegeName + "|" + scholar.getName();
                    if (ORG_NAME_URLS.contains(code)) {
                        log.info("疑似有重名，请确认=========================== {}", code + "|" + title + "|" + scholar.getWebsite());
                        continue;
                    }
                    ORG_NAME_URLS.add(code);
                    scholars.add(scholar);
                }
            }
        }

        if (!CollectionUtil.isEmpty(scholars)) {
            log.info("=============== Start saving scholars to Database. ===============");
            MongodbUtil.save(scholars);
            log.info("=============== Saving process Finished. ===============");
            MongodbUtil.delete(configure.getId(), ScholarConfigureTemp.class);
        }
        for(ScholarTemp scholar : scholars){
//            logSaver.add(scholar.getOrganizationName()+" "+scholar.getCollegeName()+" "+scholar.getName());
            StatuMap.getInstance().getStatumap().get(namehash).saver.add(scholar.getOrganizationName()+" "+scholar.getCollegeName()+" "+scholar.getName());
        }
        if(scholars.size() == 0){
            log.info(String.format("%s %s %s加载html未检测到学者信息,请手动添加",organizationName, collegeName,configure.getWebsite()));
            StatuMap.getInstance().getStatumap().get(namehash).errsaver.add(String.format("%s %s %s %s加载html未检测到学者信息,请手动添加",new Date().toString(),organizationName, collegeName,configure.getWebsite()));
            StatuMap.getInstance().getStatumap().get(namehash).errlog.add(String.format("%s %s %s %s加载html未检测到学者信息,请手动添加",new Date().toString(),organizationName, collegeName,configure.getWebsite()));
        }

        log.info("ScholarSpider ================== organizationName:{}, collegeName:{}, deptName:{}, nodes:{}, scholars:{}", organizationName, collegeName, dept, nodes.size(), scholars.size());
    }

    private ScholarTemp parseNode(Selectable node, String organizationName, String collegeName, String dept, String title, String nodeStr) {
        String name;
        String url;
        String website;

        url = node.xpath("//a/@href").toString();
        if (StringUtil.isEmpty(url)) {
            url = "";
        } else if (!url.startsWith("http")) {
            if (url.startsWith("../../")) {
                website = StringUtil.subBefore(configure.getWebsite(), '/', true);
                website = StringUtil.subBefore(website, '/', true);
                website = StringUtil.subBefore(website, '/', true);
                url = url.replaceAll("\\.\\./", "");
            } else if (url.startsWith("../")) {
                website = StringUtil.subBefore(configure.getWebsite(), '/', true);
                website = StringUtil.subBefore(website, '/', true);
                url = url.replaceAll("\\.\\./", "");
            } else if (url.startsWith("./")) {
                website = StringUtil.subBefore(configure.getWebsite(), '/', true);
                url = url.replaceAll("\\./", "");
            } else if (url.startsWith("/")) {
                website = configure.getWebsite().substring(0, configure.getWebsite().indexOf("/", 10));
            } else {
                website = StringUtil.subBefore(configure.getWebsite(), '/', true);
            }
            if (url.startsWith("/")) {
                url = website + url;
            } else {
                url = website + "/" + url;
            }
        }

        if (url.toLowerCase().contains(".jpg") || url.toLowerCase().contains(".png")) {
            return null;
        }

        ScholarTemp scholar = new ScholarTemp(organizationName, collegeName, dept, "", title, url);
        Spider.create(new DetailSpider(scholar, CrawlerUtil.detectCharset(scholar.getWebsite()))).addUrl(scholar.getWebsite()).run();

        if (scholar.getContent() != null) {
            name = getNameFromContent(scholar.getContent(), nodeStr);
            if (name != null) {
                scholar.setName(name);
                return scholar;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private String getNameFromContent(String content, String nodeStr) {
        String name;
        if (content.contains("\n") || content.contains("\r")) {
            String[] lines = content.split("[\n|\r]+");
            for (String line : lines) {
                if (!filter(line)) {
                    continue;
                }
                log.info("=============== Python INFO: Recognizing name from name string: '{}' ===============", line);
                name = CrawlerUtil.startPythonShellToGetName(line);
                log.info("=============== Python INFO: Recognizing process finished! Get scholar name: {}. ===============", name);
                if (!"None".equals(name) && name.length() > 1 && name.length() <= 4) {
                    if (nodeStr.contains(name)) {
                        return name;
                    }
                }
            }
        } else {
            log.info("=============== Python INFO: Recognizing name from name string: '{}' ===============", content);
            name = CrawlerUtil.startPythonShellToGetName(content);
            log.info("=============== Python INFO: Recognizing process finished! Get scholar name: {}. ===============", name);
            if (!"None".equals(name) && name.length() > 1 && name.length() <= 4) {
                if (nodeStr.contains(name)) {
                    return name;
                }
            }
        }
        return null;
    }

    private boolean filter(String line) {
        if (line.length() == 4 || line.length() <= 1) {
            return false;
        } else if (line.endsWith("处") || line.endsWith("部")) {
            return false;
        } else if ((line.contains("本科") || line.contains("研究生")) && line.length() < 8) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public Site getSite() {
        return site.setCharset(this.charset).addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.75 Safari/537.36");
    }

    private static class DetailSpider implements PageProcessor {

        private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setTimeOut(30000);

        private ScholarTemp scholar;
        private String charset;

        public DetailSpider(ScholarTemp scholar, String charset) {
            this.scholar = scholar;
            this.charset = charset;
        }

        @Override
        public void process(Page page) {
            Html html = page.getHtml();
            String result = CrawlerUtil.getTextTrim(html);

            if (result.contains("系统提示") && result.contains("页面") && (result.contains("不存在") || result.contains("迁移") || result.contains("出错") || result.contains("无法访问"))) {
                return;
            } else {
                scholar.setContent(result);
            }
        }

        @Override
        public Site getSite() {
            return site.setCharset(this.charset).addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.75 Safari/537.36");
        }
    }

}
