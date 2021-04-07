package com.sheep.cloud.academic.crawler.controller;

import cn.hutool.poi.excel.ExcelWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sheep.cloud.academic.crawler.entity.Scholar;
import com.sheep.cloud.academic.crawler.entity.ScholarConfigure;
import com.sheep.cloud.academic.crawler.entity.ScholarConfigureTemp;
import com.sheep.cloud.academic.crawler.entity.ScholarTemp;
import com.sheep.cloud.academic.crawler.runnable.ScholarTempRunnable;
import com.sheep.cloud.academic.crawler.service.ScholarConfigureService;
import com.sheep.cloud.academic.crawler.util.CrawlerUtil;
import com.sheep.cloud.academic.crawler.util.LogSaver;
import com.sheep.cloud.academic.crawler.vo.ScholarTempVO;
import com.sheep.cloud.academic.crawler.webmagic.ScholarDetailSpider;
import com.sheep.cloud.academic.crawler.webmagic.ScholarImgSpider;
import com.sheep.cloud.academic.crawler.webmagic.ScholarSpider;
import com.sheep.cloud.core.entity.QueryParam;
import com.sheep.cloud.core.enums.Term;
import com.sheep.cloud.core.enums.TermTypeEnum;
import com.sheep.cloud.core.util.*;
import com.sheep.cloud.open.mongodb.util.MongodbUtil;
import com.sheep.cloud.academic.crawler.util.SensitiveFilter;
import com.sheep.cloud.open.redis.util.RedisUtil;
import com.sheep.cloud.web.controller.BaseCrudController;
import com.sheep.cloud.web.controller.util.QueryParamUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.support.ui.Wait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import us.codecraft.webmagic.Spider;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author YangChao
 * @create 2020-01-13 14:23:13
 **/
@Slf4j
@RestController
@RequestMapping("scholar-temp")
@Api(tags = {"scholar-temp"}, description = "scholar-temp控制器")
public class ScholarTempController extends BaseCrudController<ScholarTemp, ScholarTempVO> {

    public static Set<String> ORG_NAME_URLS = Sets.newHashSet();

    public static Map<String, Scholar> NAME_URL_MAP = Maps.newHashMap();

    public static List<String> UPDATE_INFO = new ArrayList<>();

    private ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("scholar-pool-%d").build();

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(12, 12, 0, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(), threadFactory);

    public String parseRequest(String[] args) {
//        if (args[0].equals("load-config")) {
//            return loadConfig();
//        } else if (args[0].equals("crawler") || args[0].equals("imgCrawler") || args[0].equals("detail") || args[0].equals("detail-match")) {
//            String organizationName = args[1];
//            String collegeName = args[2];
//            QueryParam param = new QueryParam();
//            if (!organizationName.equals("all")) {
//                param.addTerm(Term.build("organizationName", organizationName));
//            }
//            if (!collegeName.equals("all")) {
//                param.addTerm(Term.build("collegeName", collegeName));
//            }
//            switch (args[0]) {
//                case "crawler":
//                    return crawler(param);
//                case "imgCrawler":
//                    return imgCrawler(param);
//                case "detail":
//                    if (!"null".equals(args[3])) {
//                        if (args[3].equals("true")) {
//                            return detail(param, true);
//                        } else {
//                            if (organizationName.equals("all") && collegeName.equals("all")) {
//                                param.addTerm(Term.build("mainPage", false));
//                            }
//                            return detail(param, false);
//                        }
//                    } else {
//                        return detail(param, false);
//                    }
//                case "detail-match":
//                    if (!"null".equals(args[3])) {
//                        if (args[3].equals("true")) {
//                            return detail(param, true);
//                        } else {
//                            if (organizationName.equals("all") && collegeName.equals("all")) {
//                                param.addTerm(Term.build("match", false));
//                            }
//                            return detailMatch(param, false);
//                        }
//                    } else {
//                        return detailMatch(param, false);
//                    }
//                default:
//                    return "Invalid Request!";
//            }
//        } else {
//            return "Invalid Request!";
//        }
        return null;
    }

    int loadConfigStatus = 0;
    int loadConfigSize = 0;
    @ApiOperation(value = "load-config状态")
    @GetMapping("/load_config_status")
    public String loadConfigStatus() {
        Map<String, Integer> map = new HashMap<>();
        map.put("progress", loadConfigStatus);
        map.put("size", loadConfigSize);
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        try {
            resultString = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
    }

    @ApiOperation(value = "添加config文件")
    @GetMapping("/load_config")
    public String loadConfig() {
        long now = System.currentTimeMillis();
        loadConfigStatus = 0;
        loadConfigSize = 0;
        /*
        String dir = FileUtil.getAbsolutePath(new File(""));
        String path = dir + "\\src\\main\\java\\com\\sheep\\cloud\\academic\\crawler\\data";
         */
        String path = "D:\\work\\workspace\\python\\buaaac\\vue-admin-template\\dist\\static\\media\\xpath";

        File file = new File(path);
        String[] fileList = file.list();
        if (fileList == null || fileList.length == 0) {
            log.info("No such file exists.");
            return null;
        } else {
            for (String fileName : fileList) {
                log.info("=============== Start loading " + fileName + " ===============");
                List<String> lines = FileUtil.readUtf8Lines(path + "//" + fileName);
                if (CollectionUtil.isEmpty(lines)) {
                    return DateUtil.millisecondToTime(now);
                }
                log.info("=============== Total lines: " + lines.size());
                loadConfigSize = lines.size();
                List<String> arrList;
                ScholarConfigure configure;
                int count = 0;
                for (String line : lines) {
                    count++;
                    loadConfigStatus = ((int)(count/(double)loadConfigSize*100));
                    if (count % 50 == 0) {
                        log.info("=============== Line count: " + count);
                    }
                    if (StringUtil.isEmpty(line)) {
                        continue;
                    }
                    if (line.startsWith("#")) {
                        continue;
                    }
                    arrList = StringUtil.split(line, '|');
                    configure = new ScholarConfigure();
                    configure.setOrganizationName(arrList.get(0));
                    configure.setCollegeName(arrList.get(1));
                    configure.setDepartmentName(arrList.get(2));
                    configure.setTitle(arrList.get(3));
                    configure.setWebsite(arrList.get(4));
                    configure.setXpath(arrList.get(5));
                    ScholarConfigure temp = BeanCopierUtil.copy(configure, ScholarConfigure.class);
                    MongodbUtil.save(temp);
                }
                log.info("=============== File: " + fileName + " finished! ===============");
                File deleteFile = new File(path + "//" + fileName);
                deleteFile.delete();
                log.info("=============== File: " + fileName + " deelted! ===============");
            }
        }
        log.info("=============== Loading file finished! ===============");
        return DateUtil.millisecondToTime(now);
    }


    int crawlerStatus = 0;
    int crawlerSize = 0;
    @ApiOperation(value = "抓取学者状态")
    @GetMapping("/crawler_status")
    public String crawlerStatus() {
        Map<String, Integer> map = new HashMap<>();
        map.put("progress", crawlerStatus);
        map.put("size", crawlerSize);
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        try {
            resultString = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
    }

    LogSaver logSaver = LogSaver.getInstance();

    @ApiOperation(value = "根据指定高校院系规则 抓取学者")
    @GetMapping("/crawler")
    public String crawler(@RequestParam(value = "organizationName", required = false,defaultValue="all") String organizationName,
                          @RequestParam(value = "collegeName", required = false,defaultValue="all") String collegeName,
                          @RequestParam(value = "refresh", required = false,defaultValue="false") boolean refresh) {
        long now = System.currentTimeMillis();
        crawlerStatus = 0;
        crawlerSize = 0;
        logSaver.clean();
        log.info("/crawler " + refresh + " "  + organizationName + " " + collegeName);
        QueryParam param = new QueryParam();
        if (!organizationName.equals("all")) {
            param.addTerm(Term.build("organizationName", organizationName));
        }
        if (!collegeName.equals("all")) {
            param.addTerm(Term.build("collegeName", collegeName));
        }
        if(!refresh){
            param.addTerm(Term.build("handled", false));
        }
        // 读取已经爬取的数据，防止再次抓取重复
        /*log.info("=============== Start reading finished scholars from scholar_temp! ===============");
        List<ScholarTemp> temps = MongodbUtil.select(param, ScholarTemp.class);
        log.info("=============== Reading process finished! Total num: " + temps.size() + ". ===============");
        if (CollectionUtil.isNotEmpty(temps)) {
            for (ScholarTemp temp : temps) {
                ORG_NAME_URLS.add(temp.getOrganizationName() + "|" + temp.getCollegeName() + "|" + temp.getName());
            }
        }
        temps.clear();*/

        // 读取抓取高校院系规则，抓取学者名称、个人主页、职称
        log.info("=============== Start loading configures from Database! ===============");
        List<ScholarConfigure> configures = MongodbUtil.select(param, ScholarConfigure.class);
        log.info("=============== Loading configures finished! Total count: " + configures.size() + " ===============");
        /*List<ScholarConfigure> configures = new ArrayList<>();
        ScholarConfigure configure1 = new ScholarConfigure("西安科技大学", "理学院", "", "教授", "https://skxy.qhnu.edu.cn/xygk/szdw.htm", "//*[@id=\"vsb_content_1117_u91\"]");
        configures.add(configure1);*/
        if (CollectionUtil.isEmpty(configures)) {
            crawlerStatus = 100;
            return DateUtil.millisecondToTime(now);
        }
        int count = 0;
        crawlerSize = configures.size();
        for (ScholarConfigure configure : configures) {
            count++;
            crawlerStatus = (int)(count/(double)crawlerSize*100) - 1;
            log.info("=============== Python INFO: Detecting web charset ······ ===============");
            String charset = CrawlerUtil.detectCharset(configure.getWebsite());
            log.info("=============== Python INFO: Detecting process finished! Get web charset: {}. ===============", charset);
            Spider.create(new ScholarSpider(configure, charset)).addUrl(configure.getWebsite()).thread(1).run();
            Update update = new Update();
            update.set("handled", true);
            MongodbUtil.patch(configure.getId(), update, ScholarConfigure.class);
            if (count % 10 == 0) {
                log.info("=============== Total count: " + configures.size() + ". ===============");
                log.info("=============== Finished num: " + count + ". ===============");
            }
        }

        log.info("=============== Total count: " + configures.size() + ". Finished: " + count + " ===============");

        count = 0;
        Map<Integer,String> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        for(String str : logSaver.getSaver()){
            result.put(count++,str);
            log.info(str);
        }
        try {
            resultString = mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        crawlerStatus = 100;
        return resultString;
    }


    int imgCrawlerStatus = 0;
    int imgCrawlerSize = 0;
    @ApiOperation(value = "抓取学者状态")
    @GetMapping("/imgCrawler_status")
    public String imgCrawlerStatus() {
        Map<String, Integer> map = new HashMap<>();
        map.put("progress", imgCrawlerStatus);
        map.put("size", imgCrawlerSize);
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        try {
            resultString = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
    }
    @ApiOperation(value = "抓取链接在图片上的学者")
    @GetMapping("/imgCrawler")
    public String imgCrawler(@RequestParam(value = "organizationName", required = false,defaultValue="all") String organizationName,
                             @RequestParam(value = "collegeName", required = false,defaultValue="all") String collegeName,
                             @RequestParam(value = "refresh", required = false,defaultValue="false") boolean refresh) {
        imgCrawlerSize = 0;
        imgCrawlerStatus = 0;
        logSaver.clean();
        log.info("/imgCrawler " + refresh + " "  + organizationName + " " + collegeName);
        QueryParam param = new QueryParam();
        if (!organizationName.equals("all")) {
            param.addTerm(Term.build("organizationName", organizationName));
        }
        if (!collegeName.equals("all")) {
            param.addTerm(Term.build("collegeName", collegeName));
        }
        if(!refresh){
            param.addTerm(Term.build("handled", false));
        }

        log.info("=============== Start loading scholars from Database! ===============");
        List<ScholarConfigureTemp> configureTemps = MongodbUtil.select(param, ScholarConfigureTemp.class);
        log.info("=============== Loading scholars succeed! Total count: " + configureTemps.size() + ". ===============");

        List<ScholarConfigure> configures = new ArrayList<>();
        for (ScholarConfigureTemp temp : configureTemps) {
            ScholarConfigure configure = BeanCopierUtil.copy(temp, ScholarConfigure.class);
            configures.add(configure);
        }
        configureTemps.clear();

        /*List<ScholarConfigure> configures = new ArrayList<>();
        ScholarConfigure configure1 = new ScholarConfigure("汕头大学", "商学院", "企业管理系", "教授", "http://swxy.tust.edu.cn/nr.aspx?id=831", "//*[@id='lm']");
        configures.add(configure1);*/

        int count = 0;
        imgCrawlerSize = configures.size();

        for (ScholarConfigure configure : configures) {
            count++;
            imgCrawlerStatus = (int)(count/(double)imgCrawlerSize*100) - 1;
            log.info("=============== Python INFO: Detecting web charset ······ ===============");
            String charset = CrawlerUtil.detectCharset(configure.getWebsite());
            log.info("=============== Python INFO: Detecting process finished! Get web charset: {}. ===============", charset);
            Spider.create(new ScholarImgSpider(configure, charset)).addUrl(configure.getWebsite()).thread(1).run();
//            Update update = new Update();
//            update.set("handled", true);
//            MongodbUtil.patch(configure.getId(), update, ScholarConfigureTemp.class);
            if (count % 10 == 0) {
                log.info("=============== Total count: " + configures.size() + ". ===============");
                log.info("=============== Finished num: " + count + ". ===============");
            }
        }

        log.info("=============== Total count: " + configures.size() + ". Finished: " + count + " ===============");

        count = 0;
        Map<Integer,String> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        for(String str : logSaver.getSaver()){
            log.info(str);
            result.put(count++,str);
        }
        try {
            resultString = mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        imgCrawlerStatus =  100;
        return resultString;
//        return "success";
    }

    @ApiOperation(value = "用于处理后续发现的一些问题")
    @GetMapping("/reHandle")
    public String reHandle() {
        QueryParam param = new QueryParam();
        //param.addTerm(Term.build("organizationName", "农业部南京农业机械化研究所"));
        //param.addTerm(Term.build("content", TermTypeEnum.NULL, null));
        log.info("=============== Start loading scholars from Database! ===============");
        List<ScholarTemp> scholars = MongodbUtil.select(param, ScholarTemp.class);
        log.info("=============== Loading scholars succeed! Total count: " + scholars.size() + ". ===============");
        for (ScholarTemp scholar : scholars) {
            if (StringUtil.isEmpty(scholar.getContent()) || scholar.getContent() == null) {
                continue;
            }
            String[] lines = scholar.getContent().split("\n");
            StringBuilder content = new StringBuilder();
            for (String line : lines) {
                if (line.contains("时间：")) {
                    continue;
                }
                content.append(line).append('\n');
            }
            Update update = new Update();
            update.set("content", content.toString());
            MongodbUtil.patch(scholar.getId(), update, ScholarTemp.class);
        }
        log.info("Finished!");
        return "success";
    }


    int detailStatus = 0;
    int detailSize = 0;
    @ApiOperation(value = "抓取学者详情状态")
    @GetMapping("/detail_status")
    public String detailStatus() {
        Map<String, Integer> map = new HashMap<>();
        map.put("progress", detailStatus);
        map.put("size", detailSize);
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        try {
            resultString = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
    }


    @ApiOperation(value = "抓取学者详情页面")
    @GetMapping(path = {"/detail"})
    public String detail(
            @RequestParam(value = "organizationName", required = false,defaultValue="all") String organizationName,
            @RequestParam(value = "collegeName", required = false,defaultValue="all") String collegeName,
            @RequestParam(value = "refresh", required = false,defaultValue="false") boolean refresh) {
        long now = System.currentTimeMillis();
        detailStatus = 0;
        detailSize = 0;
        log.info("/detail " + refresh + " " + organizationName + " " + collegeName);
        QueryParam param = new QueryParam();
        if (!organizationName.equals("all")) {
            param.addTerm(Term.build("organizationName", organizationName));
        }
        if (!collegeName.equals("all")) {
            param.addTerm(Term.build("collegeName", collegeName));
        }
        if(refresh == false){
            param.addTerm(Term.build("mainPage", false));
        }
        //param.addTerm(Term.build("organizationName", "中国农业科学院农产品加工研究所"));
        //param.addTerm(Term.build("name", "胡萍"));
        log.info("=============== Start loading scholars from Database! ===============");
        if(!refresh){
            param.addTerm(Term.build("content", TermTypeEnum.NULL, null));
        }
        List<ScholarTemp> scholars = MongodbUtil.select(param, ScholarTemp.class);
        log.info("=============== Loading scholars succeed! Total count: " + scholars.size() + ". ===============");
        detailSize = scholars.size();
        int count = 0;
        Map<Integer,String> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        if (refresh) {
            for (ScholarTemp scholar : scholars) {
                count++;
//                detailStatus = (int)(count/(double)detailSize*100);
                Update update = new Update();
                update.set("match", false);
                MongodbUtil.patch(scholar.getId(), update, ScholarTemp.class);
                if (StringUtil.isEmpty(scholar.getWebsite())) {
                    continue;
                }
                result.put(count, scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName());
                executor.execute(new ScholarSpiderRunnable(scholar));
                //Spider.create(new ScholarDetailSpider(scholar, CrawlerUtil.detectCharset(scholar.getWebsite()))).addUrl(scholar.getWebsite()).run();
            }
        } else {
            for (ScholarTemp scholar : scholars) {
                count++;
//                detailStatus = (int)(count/(double)detailSize*100);
                if (StringUtil.isEmpty(scholar.getWebsite())) {
                    continue;
                }
                result.put(count, scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName());
                executor.execute(new ScholarSpiderRunnable(scholar));

                //Spider.create(new ScholarDetailSpider(scholar, CrawlerUtil.detectCharset(scholar.getWebsite()))).addUrl(scholar.getWebsite()).run();
            }
        }
        while(executor.getQueue().size() > 0){
            detailStatus = (int)(100 - executor.getQueue().size()/(double)detailSize*100);
        }
        detailStatus  = 100;
        try {
            resultString = mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
//        return DateUtil.millisecondToTime(now);
    }

    int detailMatchStatus = 0;
    int detailMatchSize = 0;
    @ApiOperation(value = "抓取学者详情状态")
    @GetMapping("/detail_match_status")
    public String detailMatchStatus() {
        Map<String, Integer> map = new HashMap<>();
        map.put("progress", detailMatchStatus);
        map.put("size", detailMatchSize);
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        try {
            resultString = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
    }


    @ApiOperation(value = "根据学者详情页面正则匹配职称、邮箱、电话等信息")
    @GetMapping(path = {"/detail_match"})
    public String detailMatch(
            @RequestParam(value = "organizationName", required = false,defaultValue="all") String organizationName,
            @RequestParam(value = "collegeName", required = false,defaultValue="all") String collegeName,
            @RequestParam(value = "refresh", required = false,defaultValue="false") boolean refresh) {
        long now = System.currentTimeMillis();
        detailMatchStatus = 0;
        detailMatchSize = 0;
        log.info("/detail_match " + refresh + " " + organizationName + " " + collegeName);
        QueryParam param = new QueryParam();
        if (!organizationName.equals("all")) {
            param.addTerm(Term.build("organizationName", organizationName));
        }
        if (!collegeName.equals("all")) {
            param.addTerm(Term.build("collegeName", collegeName));
        }
        if(refresh == false){
            param.addTerm(Term.build("match", false));
        }
        /*
        param.addSort("organizationName", SortEnum.DESC);
        log.info("=============== Start matching detail! ===============");
        MongodbUtil.selectScroll(param, ScholarTemp.class, ScholarTempRunnable.class);
        log.info("=============== Detail match finished! ===============");
         */
        //param.addTerm(Term.build("organizationName", organizationName));
        log.info("=============== Start loading scholars from Database! ===============");
        List<ScholarTemp> scholars = MongodbUtil.select(param, ScholarTemp.class);
        log.info("=============== Loading scholars succeed! Total count: " + scholars.size() + ". ===============");
        detailMatchSize  = scholars.size();
        int count = 0;
        if (refresh) {
            for (ScholarTemp scholar : scholars) {
                count++;
//                detailMatchStatus = (int)(count/(double)detailMatchSize*100);
                Update update = new Update();
//                update.set("mainPage", false);
                update.set("match", false);
                MongodbUtil.patch(scholar.getId(), update, ScholarTemp.class);
                if (StringUtil.isEmpty(scholar.getContent()) || "null".equals(scholar.getContent())) {
                    continue;
                }
//                result.put(count, scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName());
                executor.execute(new ScholarTempRunnable(scholar));
            }
        } else {
            for (ScholarTemp scholar : scholars) {
                count++;
//                detailMatchStatus = (int)(count/(double)detailMatchSize*100);
                if (StringUtil.isEmpty(scholar.getContent()) || "null".equals(scholar.getContent())) {
                    continue;
                }
//                result.put(count++, scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName());
                executor.execute(new ScholarTempRunnable(scholar));
            }
        }
        while(executor.getQueue().size() > 0){
            detailMatchStatus = (int)(100 - executor.getQueue().size()/(double)detailMatchSize*100);
        }
        detailMatchStatus = 100;

        count = 0;
        Map<Integer,String> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        scholars = MongodbUtil.select(param, ScholarTemp.class);
        for (ScholarTemp scholar : scholars) {
            count++;
            result.put(count, scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName() + " " + scholar.getTitle() + " " + scholar.getEmail() + " " + scholar.getPhone() + " " + scholar.getId() + " " + scholar.getScholarId());
        }

        try {
            resultString = mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return resultString;
//        return UPDATE_INFO.toString();
    }

    @Autowired
    private SensitiveFilter sensitiveFilter;
    int antiCrawlerStatus = 0;
    int antiCrawlerSize = 0;
    @ApiOperation(value = "反爬虫详情状态")
    @GetMapping("/anti_crawler_status")
    public String antiCrawlerStatus() {
        Map<String, Integer> map = new HashMap<>();
        map.put("progress", antiCrawlerStatus);
        map.put("size", antiCrawlerSize);
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        try {
            resultString = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
    }

    @ApiOperation(value = "反爬虫详情")
    @GetMapping(path = {"/anti_crawler"})
    public String antiCrawler(
            @RequestParam(value = "organizationName", required = false,defaultValue="all") String organizationName,
            @RequestParam(value = "collegeName", required = false,defaultValue="all") String collegeName) {
        long now = System.currentTimeMillis();
        antiCrawlerStatus = 0;
        antiCrawlerSize = 0;
        log.info("/anti_crawler " + organizationName + " " + collegeName);
        QueryParam param = new QueryParam();
        if (!organizationName.equals("all")) {
            param.addTerm(Term.build("organizationName", organizationName));
        }
        if (!collegeName.equals("all")) {
            param.addTerm(Term.build("collegeName", collegeName));
        }
        log.info("=============== Start loading scholars from Database! ===============");
        List<ScholarTemp> scholars = MongodbUtil.select(param, ScholarTemp.class);
        log.info("=============== Loading scholars succeed! Total count: " + scholars.size() + ". ===============");
        antiCrawlerSize = scholars.size();
        Map<Integer,String> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        int count = 0;
        for (ScholarTemp scholar : scholars) {
            if(StringUtil.isEmpty(scholar.getContent())){
                continue;
            }
            String content = scholar.getContent();
            if(sensitiveFilter.find(content)) {
                antiCrawlerStatus  = (int)(count/(double)antiCrawlerSize*100);
                result.put(count++, scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName());
//                log.info("重新爬取" + scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName());
//                Spider.create(new ScholarDetailSpider(scholar, CrawlerUtil.detectCharset(scholar.getWebsite()))).addUrl(scholar.getWebsite()).run();
            }
        }
        antiCrawlerSize = result.size();
        while(executor.getQueue().size() > 0){
            antiCrawlerStatus = (int)(100 - executor.getQueue().size()/(double)antiCrawlerSize*100);
        }

        antiCrawlerStatus  = 100;
        try {
            resultString = mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
    }

    @GetMapping("/update_status")
    public void updateStatus(){
        logSaver.clean();
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


    @GetMapping("/scholar_update")
    public String scholarUpdate() {
        long now = System.currentTimeMillis();
        List<Scholar> scholars = MongodbUtil.findAll(Scholar.class);
        String organizationName;
        String collegeName;
        String dept;
        String name;
        String website;
        for (Scholar scholar : scholars) {
            name = scholar.getName();
            organizationName = scholar.getOrganizationName();
            collegeName = scholar.getCollegeName() == null ? "" : scholar.getCollegeName();
            dept = scholar.getDepartmentName() == null ? "" : scholar.getDepartmentName();
            website = scholar.getWebsite();
            if (StringUtil.isEmpty(name)) {
                continue;
            }
            NAME_URL_MAP.put(organizationName + "=" + collegeName + "=" + dept + "=" + name, scholar);
            if (StringUtil.isNotEmpty(website)) {
                NAME_URL_MAP.put(website, scholar);
            }
        }
        List<ScholarTemp> temps = MongodbUtil.findAll(ScholarTemp.class);
        for (ScholarTemp temp : temps) {
            name = temp.getName();
            if (name.length() >= 4) {
                continue;
            }
            organizationName = temp.getOrganizationName();
            collegeName = temp.getCollegeName() == null ? "" : temp.getCollegeName();
            dept = temp.getDepartmentName() == null ? "" : temp.getDepartmentName();
            website = temp.getWebsite();
            if (StringUtil.isNotEmpty(website) && NAME_URL_MAP.containsKey(website)) {
                Scholar scholar = NAME_URL_MAP.get(website);
                if ("研究员".equals(temp.getTitle()) && (!"院士".equals(scholar.getTitle()) || !"研究员".equals(scholar.getTitle()))) {
                    Update update = new Update();
                    update.set("beenFlag", "update");
                    update.set("scholarId", scholar.getId());
                    MongodbUtil.patch(temp.getId(), update, ScholarTemp.class);
                }
            } else if (NAME_URL_MAP.containsKey(organizationName + "=" + collegeName + "=" + dept + "=" + name)) {
                Scholar scholar = NAME_URL_MAP.get(organizationName + "=" + collegeName + "=" + dept + "=" + name);
                if ("研究员".equals(temp.getTitle()) && (!"院士".equals(scholar.getTitle()) || !"研究员".equals(scholar.getTitle()))) {
                    Update update = new Update();
                    update.set("scholarId", scholar.getId());
                    update.set("beenFlag", "update");
                    MongodbUtil.patch(temp.getId(), update, ScholarTemp.class);
                }
            } else {
                Update update = new Update();
                update.set("beenFlag", "insert");
                MongodbUtil.patch(temp.getId(), update, ScholarTemp.class);
            }
        }
        return DateUtil.millisecondToTime(now);
    }

    @GetMapping(path = {"/merge"})
    public String merge() {
        long now = System.currentTimeMillis();
        QueryParam param = new QueryParam();
        param.addTerm(Term.build("beenFlag", "insert"));
        List<ScholarTemp> temps = MongodbUtil.select(param, ScholarTemp.class);
        MongodbUtil.save(BeanCopierUtil.copys(temps, Scholar.class));

        param = new QueryParam();
        param.addTerm(Term.build("beenFlag", "update"));
        temps = MongodbUtil.select(param, ScholarTemp.class);
        List<Scholar> updateLists = Lists.newArrayList();
        for (ScholarTemp temp : temps) {
            Scholar update = new Scholar();
            update.setId(temp.getScholarId());
            update.setTitle(temp.getTitle());
            update.setDepartmentName(temp.getDepartmentName());
            updateLists.add(update);
        }
        MongodbUtil.patch(updateLists);
        return DateUtil.millisecondToTime(now);
    }

    @GetMapping("/scholar_file")
    public String scholarFile() {
        long now = System.currentTimeMillis();
        List<String> lines = FileUtil.readUtf8Lines("D:\\采集大学老师列表.txt");
        List<String> arrList;
        String organizationName;
        String collegeName;
        String departmentName;
        String name;
        String url;
        String title;
        List<ScholarTemp> scholars = Lists.newArrayList();
        ScholarTemp scholar;
        for (String line : lines) {
            if (StringUtil.isEmpty(line)) {
                continue;
            }
            arrList = StringUtil.split(line, '|');
            if (arrList.size() != 6) {
                System.out.println("line ================== " + line + " 数据异常");
                continue;
            }
            organizationName = arrList.get(0);
            collegeName = arrList.get(1);
            departmentName = arrList.get(2);
            name = arrList.get(4);
            title = arrList.get(3);
            url = arrList.get(5);

            scholar = new ScholarTemp();
            scholar.setName(CrawlerUtil.replace(name));
            scholar.setWebsite(url);
            scholar.setTitle(title);
            scholar.setOrganizationName(organizationName);
            scholar.setCollegeName(collegeName);
            scholar.setDepartmentName(departmentName);
            scholars.add(scholar);
        }
        List<ScholarTemp> list = MongodbUtil.save(scholars);
        for (ScholarTemp temp : list) {
            if (StringUtil.isEmpty(temp.getWebsite())) {
                continue;
            }
            Spider.create(new ScholarDetailSpider(temp, temp.getWebsite())).addUrl(temp.getWebsite()).run();
        }
        return DateUtil.millisecondToTime(now);
    }

    @GetMapping("/test")
    public String test() {
        long now = System.currentTimeMillis();
        List<ScholarTemp> temps = MongodbUtil.findAll(ScholarTemp.class);
        List<ScholarTemp> updateList = Lists.newArrayList();
        ScholarTemp update;
        for (ScholarTemp temp : temps) {
            update = new ScholarTemp();
            update.setId(temp.getId());
            if (StringUtil.isEmpty(temp.getTitle())) {
                update.setTitle("null");
            }
            if (StringUtil.isEmpty(temp.getContent())) {
                update.setContent("null");
            }
            if (StringUtil.isEmpty(temp.getEmail())) {
                update.setEmail("null");
            }
            if (StringUtil.isEmpty(temp.getPhone())) {
                update.setPhone("null");
            }
            updateList.add(update);
        }
        MongodbUtil.patch(updateList);
        return DateUtil.millisecondToTime(now);
    }

    @GetMapping("/scholar_compare")
    public String scholarCompare() {
        long now = System.currentTimeMillis();
        List<String> lines = FileUtil.readUtf8Lines("C:\\Users\\YangChao\\Desktop\\scholar_temp.txt");
        List<String> arrList;
        Set<String> sets = Sets.newHashSet();
        for (String line : lines) {
            if (StringUtil.isEmpty(line)) {
                continue;
            }
            arrList = StringUtil.split(line, '|');
            if (arrList.size() != 4) {
                continue;
            }
            sets.add(arrList.get(0) + "|" + arrList.get(1) + "|" + arrList.get(2));
            if (StringUtil.isNotEmpty(arrList.get(3))) {
                sets.add(arrList.get(3));
            }
        }

        lines = FileUtil.readUtf8Lines("C:\\Users\\YangChao\\Desktop\\scholar.txt");
        List<String> insertList = Lists.newArrayList();
        for (String line : lines) {
            if (StringUtil.isEmpty(line)) {
                continue;
            }
            arrList = StringUtil.split(line, '|');
            if (arrList.size() != 6) {
                continue;
            }
            if (sets.contains(arrList.get(0) + "|" + arrList.get(1) + "|" + arrList.get(4))) {
                continue;
            }
            if (StringUtil.isNotEmpty(arrList.get(5)) && sets.contains(arrList.get(5))) {
                continue;
            }
            insertList.add(line);
        }

        FileUtil.appendUtf8Lines(insertList, "C:\\Users\\YangChao\\Desktop\\scholar_0813.txt");

        return DateUtil.millisecondToTime(now);
    }

    @GetMapping("/summary")
    public String summary() {
        long now = System.currentTimeMillis();
        List<String> lines = FileUtil.readUtf8Lines("C:\\Users\\YangChao\\Desktop\\scholar_temp.txt");
        Map<String, Integer> map = Maps.newHashMap();
        Integer count;
        for (String line : lines) {
            if (StringUtil.isEmpty(line)) {
                continue;
            }
            count = map.getOrDefault(line, 0);
            map.put(line, count + 1);
        }
        List<String> list = Lists.newArrayList();
        for (String key : map.keySet()) {
            list.add(key + "|" + map.get(key));
        }
        list.sort(String::compareToIgnoreCase);
        FileUtil.appendUtf8Lines(list, "C:\\Users\\YangChao\\Desktop\\summary0825.txt");
        return DateUtil.millisecondToTime(now);
    }

    private class ScholarSpiderRunnable implements Runnable {

        ScholarTemp scholar;

        public ScholarSpiderRunnable(ScholarTemp scholar) {
            this.scholar = scholar;
        }

        @Override
        public void run() {
            Spider.create(new ScholarDetailSpider(scholar, CrawlerUtil.detectCharset(scholar.getWebsite()))).addUrl(scholar.getWebsite()).run();
            //Spider.create(new ScholarDetailSpider(scholar, "GBK")).addUrl(scholar.getWebsite()).run();
        }
    }

    @Autowired
    private ScholarConfigureService scholarConfigureService;



    @GetMapping("/analyzeNum")
    public String analyzeNum() {
        log.info("=============== Start loading scholars from Database! ===============");
        List<ScholarTemp> scholars = MongodbUtil.select(new QueryParam(), ScholarTemp.class);
        log.info("=============== Loading scholars succeed! Total count: " + scholars.size() + ". ===============");

        Set<String> ORG_Col = Sets.newHashSet();
        List<UniversityScholar> list = new ArrayList<>();

        for (ScholarTemp scholar : scholars) {
            String code = scholar.getOrganizationName() + '|' + scholar.getCollegeName();
            if (!ORG_Col.contains(code)) {
                UniversityScholar universityScholar = new UniversityScholar(scholar.getOrganizationName(), scholar.getCollegeName());
                universityScholar.addNum();
                list.add(universityScholar);
                ORG_Col.add(code);
            } else {
                for (UniversityScholar universityScholar : list) {
                    if (scholar.getOrganizationName().equals(universityScholar.getOrganizationName()) && scholar.getCollegeName().equals(universityScholar.getCollegeName())) {
                        universityScholar.addNum();
                    }
                }
            }
        }

        log.info("=============== Start writing file. ===============");
        File file = new File("E:\\非985211各院校各学院学者人数统计.xlsx");
        ExcelWriter writer = ExcelUtil.getWriter(file);
        writer.addHeaderAlias("organizationName", "大学名称");
        writer.addHeaderAlias("collegeName", "学院名称");
        writer.addHeaderAlias("num", "学者人数");
        writer.write(list);
        writer.flush();
        log.info("=============== Writing file finished! ===============");
        writer.close();
        return "success";
    }

    @Data
    private static class UniversityScholar {
        private String organizationName;
        private String collegeName;
        private int num;

        public UniversityScholar(String organizationName, String collegeName) {
            this.organizationName = organizationName;
            this.collegeName = collegeName;
        }

        private void addNum() {
            this.num++;
        }
    }

    @GetMapping("/export-to-excel")
    public String exportToExcel() {
        log.info("=============== Start loading scholars from Database! ===============");
        List<ScholarTemp> scholars = MongodbUtil.select(new QueryParam(), ScholarTemp.class);
        log.info("=============== Loading scholars succeed! Total count: " + scholars.size() + ". ===============");

        Set<String> ORG = Sets.newHashSet();
        List<UniversityCollege> list = new ArrayList<>();

        for (ScholarTemp scholar : scholars) {
            String code = scholar.getOrganizationName();
            String collegeName = (StringUtil.isBlank(scholar.getCollegeName())) ? "None" : scholar.getCollegeName();
            if (!ORG.contains(code)) {
                UniversityCollege universityCollege = new UniversityCollege(code);
                universityCollege.addCollege(collegeName);
                list.add(universityCollege);
                ORG.add(code);
            } else {
                for (UniversityCollege universityCollege : list) {
                    if (scholar.getOrganizationName().equals(universityCollege.getOrganizationName())) {
                        if (!universityCollege.getColleges().contains(collegeName)) {
                            universityCollege.addCollege(collegeName);
                        }
                    }
                }
            }
        }

        log.info("=============== Start writing file. ===============");
        File file = new File("E:\\非985211各院校学院数统计.xlsx");
        ExcelWriter writer = ExcelUtil.getWriter(file);
        writer.addHeaderAlias("organizationName", "大学名称");
        writer.addHeaderAlias("num", "学院数");
        writer.write(list);
        writer.flush();
        log.info("=============== Writing file finished! ===============");
        writer.close();
        return "success";
    }

    @Data
    private static class UniversityCollege {
        private String organizationName;
        private List<String> colleges;
        private int num;

        public UniversityCollege(String organizationName) {
            this.organizationName = organizationName;
            this.colleges = new ArrayList<>();
        }

        public void addCollege(String collegeName) {
            this.colleges.add(collegeName);
            this.num = this.colleges.size();
        }
    }

}
