package com.sheep.cloud.academic.crawler.webmagic;

import com.sheep.cloud.academic.crawler.entity.ScholarConfigure;
import com.sheep.cloud.academic.crawler.entity.ScholarConfigureTemp;
import com.sheep.cloud.academic.crawler.entity.ScholarTemp;
import com.sheep.cloud.academic.crawler.util.CrawlerUtil;
import com.sheep.cloud.academic.crawler.util.LogSaver;
import com.sheep.cloud.core.util.BeanCopierUtil;
import com.sheep.cloud.core.util.CollectionUtil;
import com.sheep.cloud.core.util.StringUtil;
import com.sheep.cloud.open.mongodb.util.MongodbUtil;
import com.sheep.cloud.open.redis.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.sheep.cloud.academic.crawler.controller.ScholarTempController.ORG_NAME_URLS;
import static com.sheep.cloud.core.constants.PatternConstant.EMAIL;

/**
 * @author YangChao
 * @create 2019-05-05 15:31
 **/


@Slf4j
public class ScholarSpider implements PageProcessor {

    public static Pattern PATTERN = Pattern.compile("[0-9a-zA-Z]");

    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setTimeOut(30000);

    private ScholarConfigure configure;

    private String charset;

    LogSaver logSaver = LogSaver.getInstance();

    public ScholarSpider(ScholarConfigure configure, String charset) {
        this.configure = configure;
        this.charset = charset;
    }

    @Override
    public void process(Page page) {
        Html html = page.getHtml();
        String organizationName = configure.getOrganizationName();
        String collegeName = configure.getCollegeName();
        String website;
        String dept = StringUtil.isEmpty(configure.getDepartmentName()) ? "" : configure.getDepartmentName();

        Selectable selectable = html.xpath(configure.getXpath());
        List<Selectable> nodes = selectable.xpath("//a").nodes();
        /*
         * 提取没有链接的学者姓名
         */
        if (CollectionUtil.isEmpty(nodes) || allEmailLink(nodes)) {
            String nameStr = CrawlerUtil.getTextTrim(selectable);
            List<String> nameList = new ArrayList<>();
            if (nameStr.contains("\n")) {
                String[] names = nameStr.split("\n");
                for (String name : names) {
                    if (!StringUtil.isEmpty(name)) {
                        log.info("=============== Python INFO: Recognizing name from name string: '{}' ===============", name);
                        List<String> list = CrawlerUtil.startPythonShellToGetAllName(name);
                        log.info("=============== Python INFO: Recognizing process finished! Get scholar name: {}. ===============", list.toString());
                        if (!list.isEmpty()) {
                            nameList.addAll(list);
                        }
                    }
                }
            } else {
                if (!StringUtil.isEmpty(nameStr)) {
                    log.info("=============== Python INFO: Recognizing name from name string: '{}' ===============", nameStr);
                    nameList = CrawlerUtil.startPythonShellToGetAllName(nameStr);
                    log.info("=============== Python INFO: Recognizing process finished! Get scholar name: {}. ===============", nameList.toString());
                }
            }
            List<ScholarTemp> scholars = new ArrayList<>();
            for (String name : nameList) {
                if (StringUtil.isBlank(name)) {
                    continue;
                }
                if (PATTERN.matcher(name).find()) {
                    continue;
                }
                if (name.length() > 4 || name.length() <= 1) {
                    continue;
                }
                String code = organizationName + "|" + collegeName + "|" + name;
                String title = StringUtil.isEmpty(configure.getTitle()) ? "" : configure.getTitle();
                if (ORG_NAME_URLS.contains(code)) {
                    log.info("疑似有重名，请确认=========================== {}", code + "|" + title);
                    continue;
                }
                ORG_NAME_URLS.add(code);
                scholars.add(new ScholarTemp(organizationName, collegeName, dept, name, title, ""));
            }
            if (CollectionUtil.isEmpty(scholars)) {
                ScholarConfigureTemp temp = BeanCopierUtil.copy(configure, ScholarConfigureTemp.class);
                MongodbUtil.save(temp);
            } else {
                log.info("=============== Start saving scholars to Database. ===============");
                MongodbUtil.save(scholars);
                log.info("=============== Saving process Finished. ===============");
            }
            log.info("ScholarSpider ======================= organizationName:{}, collegeName:{}, deptName:{}, nodes:{}, scholars:{}", organizationName, collegeName, dept, nodes.size(), scholars.size());

            return;
        }

        String name;
        String title;
        String url;
        List<ScholarTemp> scholars = new ArrayList<>();
        String code;
        for (Selectable node : nodes) {
            // 解析个人主页
            url = node.xpath("//a/@href").toString();
            // 解析个人姓名
            name = CrawlerUtil.getTextTrim(node);
            if (StringUtil.isBlank(name)) {
                name = CrawlerUtil.replace(node.xpath("//a/@title").toString());
            }
            if (StringUtil.isBlank(name)) {
                name = CrawlerUtil.replace(node.xpath("//a/@textvalue").toString());
            }
            if (StringUtil.isBlank(name)) {
                continue;
            }
            if (name.length() > 3) {
                if (name.contains("副教授")) {
                    name = name.replaceAll("([(（【<]|\\[)?副教授([)）】>]|])?", "");
                    title = "副教授";
                } else if (name.contains("助理教授")) {
                    name = name.replaceAll("([(（【<]|\\[)?助理教授([)）】>]|])?", "");
                    title = "助理教授";
                } else if (name.contains("教授")) {
                    name = name.replaceAll("([(（【<]|\\[)?教授([)）】>]|])?", "");
                    title = "教授";
                } else if (name.contains("讲师")) {
                    name = name.replaceAll("([(（【<]|\\[)?讲师([)）】>]|])?", "");
                    title = "讲师";
                } else if (name.contains("高级工程师")) {
                    name = name.replaceAll("([(（【<]|\\[)?高级工程师([)）】>]|])?", "");
                    title = "高级工程师";
                } else if (name.contains("高级实验师")) {
                    name = name.replaceAll("([(（【<]|\\[)?高级实验师([)）】>]|])?", "");
                    title = "高级实验师";
                } else if (name.contains("助理实验师")) {
                    name = name.replaceAll("([(（【<]|\\[)?助理实验师([)）】>]|])?", "");
                    title = "助理实验师";
                } else if (name.contains("实验师")) {
                    name = name.replaceAll("([(（【<]|\\[)?实验师([)）】>]|])?", "");
                    title = "实验师";
                } else if (name.contains("副研究员")) {
                    name = name.replaceAll("([(（【<]|\\[)?副研究员([)）】>]|])?", "");
                    title = "副研究员";
                } else if (name.contains("助理研究员")) {
                    name = name.replaceAll("([(（【<]|\\[)?助理研究员([)）】>]|])?", "");
                    title = "助理研究员";
                } else if (name.contains("研究员")) {
                    name = name.replaceAll("([(（【<]|\\[)?研究员([)）】>]|])?", "");
                    title = "研究员";
                } else if (name.contains("助教")) {
                    name = name.replaceAll("([(（【<]|\\[)?助教([)）】>]|])?", "");
                    title = "助教";
                } else {
                    title = StringUtil.isEmpty(configure.getTitle()) ? "" : configure.getTitle();
                }
            } else {
                title = StringUtil.isEmpty(configure.getTitle()) ? "" : configure.getTitle();
            }
            name = formatName(name);
            String nameWaitingHandle = name;
            if (StringUtil.isBlank(name)) {
                continue;
            }
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
            if (name.length() <= 1) {
                continue;
            }
            if (name.length() > 3 || name.contains("-") || PATTERN.matcher(name).find()) {
                log.info("=============== Python INFO: Recognizing name from name string: '{}' ===============", nameWaitingHandle);
                name = CrawlerUtil.startPythonShellToGetName(nameWaitingHandle);
                log.info("=============== Python INFO: Recognizing process finished! Get scholar name: {}. ===============", name);
                if (name.equals("None") || name.length() <= 1 || name.length() > 4) {
                    continue;
                }
            }
            if (PATTERN.matcher(name).find()) {
                continue;
            }
            code = organizationName + "|" + collegeName + "|" + name;
//            if (ORG_NAME_URLS.contains(code) || ORG_NAME_URLS.contains(url)) {
//                log.info("疑似有重名，请确认=========================== {}", code + "|" + title + "|" + url);
//                continue;
//            }
//            ORG_NAME_URLS.add(code);
//            ORG_NAME_URLS.add(url);

            //有些url是用js代码表示的，不同的人对应的代码是相同的，加入url舍弃了太多，反倒同一学校同一学院学者重名的概率较小
            if (ORG_NAME_URLS.contains(code)) {
                log.info("疑似有重名，请确认=========================== {}", code + "|" + title + "|" + url);
                continue;
            }
            ORG_NAME_URLS.add(code);
            scholars.add(new ScholarTemp(organizationName, collegeName, dept, name, title, url));
        }
        if (CollectionUtil.isEmpty(scholars)) {
            ScholarConfigureTemp temp = BeanCopierUtil.copy(configure, ScholarConfigureTemp.class);
            MongodbUtil.save(temp);
        } else {
            log.info("=============== Start saving scholars to Database. ===============");
            MongodbUtil.save(scholars);
            log.info("=============== Saving process Finished. ===============");
            //RedisUtil.setRemove("crawler:scholar:website", configure.getId());
        }
        for(ScholarTemp scholar : scholars){
            logSaver.add(scholar.getOrganizationName()+" "+scholar.getCollegeName()+" "+scholar.getName());
        }
        log.info("ScholarSpider ======================= organizationName:{}, collegeName:{}, deptName:{}, nodes:{}, scholars:{}", organizationName, collegeName, dept, nodes.size(), scholars.size());
    }

    private boolean allEmailLink(List<Selectable> nodes) {
        for (Selectable node : nodes) {
            if (!EMAIL.matcher(node.xpath("//a/@href").toString()).find()) {
                return false;
            }
        }
        return true;
    }

    public static String formatName(String name) {
        if ("教授".equals(name) || "副教授".equals(name) || "讲师".equals(name) || "职称".equals(name) || "博导".equals(name) || "兼职博导".equals(name) || "讲座".equals(name) || "辅导员".equals(name) || "行政人员".equals(name) || "师资队伍".equals(name)) {
            return null;
        }
        if ("首页".equals(name) || "下一页".equals(name) || "上一页".equals(name) || "下页".equals(name) || "上页".equals(name) || "末页".equals(name) || "第一页".equals(name) || "尾页".equals(name) || "最后页".equals(name) || "下5页".equals(name) || "跳转到".equals(name)) {
            return null;
        }
        if ("当前页".equals(name) || "硕导".equals(name) || "跳转".equals(name) || "详情".equals(name) || "详细".equals(name) || "查看详情".equals(name) || "查看详细".equals(name) || "查看".equals(name)) {
            return null;
        }
        name = name.replaceAll("个人简介", "");
        name = name.replaceAll("教师简介", "");
        name = name.replaceAll("老师简介", "");
        name = name.replaceAll("简介", "");
        name = name.replaceAll("老师个人简介", "");
        name = name.replaceAll("（博导）", "");
        name = name.replaceAll("博士生导师", "");
        name = name.replaceAll("师资队伍", "");
        name = name.replaceAll("浏览更多", "");
        name = name.replaceAll("详情", "");
        name = name.replaceAll("详细", "");
        name = name.replaceAll("特别研究员", "研究员");
        name = name.replaceAll("首席研究员", "研究员");
        name = name.replaceAll("中国工程院院士", "");
        name = name.replaceAll("中国科学院院士", "");
        name = name.replaceAll("设计大师", "");
        name = name.replaceAll(" ", "");
        name = name.replaceAll("　", "");
        name = name.replaceAll("进入主页", "");
        name = name.replaceAll("研究方向", "");
        name = name.replaceAll("了解更多", "");
        name = name.replaceAll("更多", "");
        name = name.replaceAll("姓名：", "");
        name = name.replaceAll("性别：男", "");
        name = name.replaceAll("性别：女", "");
        name = name.replaceAll("聘任职务：", "");
        name = name.replaceAll("特聘教授", "");
        name = name.replaceAll("博士，", "");
        name = name.replaceAll("，", "");
        name = name.replaceAll(",", "");
        name = name.replaceAll("/", "");
        name = name.replaceAll(" 、", "");
        name = name.replaceAll("\n", "");
        name = name.replaceAll("\\*", "");
        name = name.replaceAll("\\d{2,4}([\\-|\\.]\\d{1,2}){1,2}", "");
        name = name.replaceAll("\\d+", "");
//        if (name.contains("（")) {
//            name = StringUtil.subBefore(name, "（", false);
//        }
//        if (name.contains("(")) {
//            name = StringUtil.subBefore(name, "(", false);
//        }
//        if (name.contains("[")) {
//            name = StringUtil.subBefore(name, "[", false);
//        }
//        if (name.contains("-")) {
//            String[] str = name.split("-");
//            if (str.length == 1) {
//                name = str[0];
//            } else {
//                String[] titles = {"副主任", "主任", "副院长", "院长", "院士", "副教授", "助理教授", "教授", "讲师", "高级工程师", "高级实验师", "助理实验师", "实验师", "助理研究员", "研究员", "副研究员", "助教"};
//                boolean flag = true;
//                for (String title : titles) {
//                    if (str[0].equals(title)) {
//                        flag = false;
//                        name = str[1];
//                        break;
//                    }
//                }
//                if (flag) {
//                    name = str[0];
//                }
//            }
//        }
        return name;
    }

    @Override
    public Site getSite() {
        return site.setCharset(this.charset).addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.75 Safari/537.36");
    }

}