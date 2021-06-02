package com.sheep.cloud.academic.crawler.controller;

import cn.hutool.poi.excel.ExcelWriter;
import com.alibaba.fastjson.JSONPath;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sheep.cloud.academic.crawler.entity.*;
import com.sheep.cloud.academic.crawler.runnable.ScholarTempRunnable;
import com.sheep.cloud.academic.crawler.service.ScholarConfigureService;
import com.sheep.cloud.academic.crawler.util.*;
import com.sheep.cloud.academic.crawler.vo.ScholarTempVO;
import com.sheep.cloud.academic.crawler.webdriver.MyPhantomJsDriver;
import com.sheep.cloud.academic.crawler.webmagic.*;
import com.sheep.cloud.core.entity.QueryParam;
import com.sheep.cloud.core.enums.Term;
import com.sheep.cloud.core.enums.TermTypeEnum;
import com.sheep.cloud.core.util.*;
import com.sheep.cloud.open.mongodb.util.MongodbUtil;
import com.sheep.cloud.open.redis.util.RedisUtil;
import com.sheep.cloud.web.controller.BaseCrudController;
import com.sheep.cloud.web.controller.util.QueryParamUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Wait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
//import us.codecraft.webmagic.Spider;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;


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
        return null;
    }
    ConcurrentHashMap<String, InnerStatu> statumap = StatuMap.getInstance().getStatumap();

//    int loadConfigStatus = 0;
//    int loadConfigSize = 0;
    @ApiOperation(value = "load-config状态")
    @GetMapping("/load_config_status")
    public String loadConfigStatus(@RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        Map<String, Long> map = new HashMap<>();
        map.put("progress", statu.loadConfigStatus);
        map.put("size", statu.loadConfigSize);
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        try {
            resultString = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
    }
//    HashSet<String> set = new HashSet<>();
    @ApiOperation(value = "添加config文件")
    @GetMapping("/load_config")
    public String loadConfig(@RequestParam(value = "name", required = true) String name) {
        long now = System.currentTimeMillis();
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        statu.loadConfigStatus = 0;
        statu.loadConfigSize = 0;
//        loadConfigStatus = 0;
//        loadConfigSize = 0;
        /*
        String dir = FileUtil.getAbsolutePath(new File(""));
        String path = dir + "\\src\\main\\java\\com\\sheep\\cloud\\academic\\crawler\\data";
         */
        String path = "D:\\work\\workspace\\python\\buaaac\\vue-admin-template\\dist\\static\\media\\xpath";
//        String path = "/home/xzpc/buaaac/vue-admin-template/dist/static/media/xpath";
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
//                loadConfigSize = lines.size();
                statu.loadConfigSize = lines.size();
                List<String> arrList;
                ScholarConfigure configure;
                int count = 0;
                for (String line : lines) {
                    count++;
                    statu.loadConfigStatus = ((long)(count/(double)statu.loadConfigSize * 100));
//                    loadConfigStatus = ((int)(count/(double)loadConfigSize*100));
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
                    configure.setWriteBy(name);
                    ScholarConfigure temp = BeanCopierUtil.copy(configure, ScholarConfigure.class);
                    MongodbUtil.save(temp);
                    statu.set.add(arrList.get(0) + " " + arrList.get(1));
//                    set.add(arrList.get(0) + " " + arrList.get(1));
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

    @ApiOperation(value = "新增抓取学者接口")
    @GetMapping("/crawler")
    public String dynamicCrawler(@RequestParam(value = "organizationName", required = false,defaultValue="all") String organizationName,
                                 @RequestParam(value = "collegeName", required = false,defaultValue="all") String collegeName,
                                 @RequestParam(value = "refresh", required = false,defaultValue="false") boolean refresh,
                                 @RequestParam(value = "name", required = true) String name) {

        log.info("=============== Start reading finished scholars from database! ===============");
        List<ScholarTemp> temps = MongodbUtil.select(new QueryParam(), ScholarTemp.class);
        log.info("=============== Reading process finished! Total num: " + temps.size() + ". ===============");
        if (CollectionUtil.isNotEmpty(temps)) {
            for (ScholarTemp temp : temps) {
                ORG_NAME_URLS.add(temp.getOrganizationName() + "|" + temp.getCollegeName() + "|" + temp.getName());
            }
        }
        temps.clear();
        //去重步骤
        long now = System.currentTimeMillis();
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        statu.crawlerSize = 0 ;
        statu.crawlerStatus = 0;
        statu.logclear();
        log.info("/crawler " + refresh + " "  + organizationName + " " + collegeName + " " + name);
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
        param.addTerm(Term.build("writeBy", name));

        log.info("=============== Start loading configures from database! ===============");
        List<ScholarConfigure> configures = MongodbUtil.select(param, ScholarConfigure.class);
        log.info("=============== Loading configures finished! Total count: " + configures.size() + " ===============");

        if (CollectionUtil.isEmpty(configures)) {
            statu.crawlerStatus = 100;
            return DateUtil.millisecondToTime(now);
        }

        int count = 0;
        statu.crawlerSize = configures.size();
        WebDriver driver = MyPhantomJsDriver.getPhantomJSDriver();
        for (ScholarConfigure configure : configures) {
            count++;
            statu.crawlerStatus = (long)(count/(double)statu.crawlerSize*100) - 1;
            log.info("=============== Python INFO: Detecting web charset ······ ===============");
            String charset = CrawlerUtil.detectCharset(configure.getWebsite());
            log.info("=============== Python INFO: Detecting process finished! Get web charset: {}. ===============", charset);
            new DynamicScholarSpider(configure, charset, driver, name).run();
            if (count % 10 == 0) {
                log.info("=============== Total count: " + configures.size() + ". ===============");
                log.info("=============== Finished num: " + count + ". ===============");
            }
            Update update = new Update();
            update.set("handled", true);
            MongodbUtil.patch(configure.getId(), update, ScholarConfigure.class);
        }

        log.info("=============== Total count: " + configures.size() + ". Finished: " + count + " ===============");
//        return "finish!";


        count = 0;
        Map<Integer,String> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        for(String str : statu.saver){
            result.put(count++,str);
            log.info(str);
        }
        try {
            resultString = mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        statu.crawlerStatus = 100;
        return resultString;
    }

    @GetMapping("/englishDynamicCrawler")
    public String englishDynamicCrawler() {
        log.info("=============== Start loading configures from Database! ===============");
        List<ScholarConfigure> configures = MongodbUtil.select(new QueryParam(), ScholarConfigure.class);
        log.info("=============== Loading configures finished! Total count: " + configures.size() + " ===============");

        if (CollectionUtil.isEmpty(configures)) {
            return "no data";
        }

        int count = 0;
        WebDriver driver = MyPhantomJsDriver.getPhantomJSDriver();
        for (ScholarConfigure configure : configures) {
            count++;
            log.info("=============== Python INFO: Detecting web charset ······ ===============");
            String charset = CrawlerUtil.detectCharset(configure.getWebsite());
            log.info("=============== Python INFO: Detecting process finished! Get web charset: {}. ===============", charset);
            us.codecraft.webmagic.Spider.create(new EnglishScholarSpider(configure, charset, driver)).addUrl(configure.getWebsite()).thread(1).run();
            if (count % 10 == 0) {
                log.info("=============== Total count: " + configures.size() + ". ===============");
                log.info("=============== Finished num: " + count + ". ===============");
            }
        }

        log.info("=============== Total count: " + configures.size() + ". Finished: " + count + " ===============");
        return "finish!";
    }


//    int crawlerStatus = 0;
//    int crawlerSize = 0;
    @ApiOperation(value = "抓取学者状态")
    @GetMapping("/crawler_status")
    public String crawlerStatus(@RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        Map<String, Long> map = new HashMap<>();
        map.put("progress", statu.crawlerStatus);
        map.put("size", statu.crawlerSize);
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        try {
            resultString = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
    }

//    LogSaver logSaver = LogSaver.getInstance();

//    @ApiOperation(value = "根据指定高校院系规则 抓取学者")
//    @GetMapping("/crawler1")
//    public String crawler(@RequestParam(value = "organizationName", required = false,defaultValue="all") String organizationName,
//                          @RequestParam(value = "collegeName", required = false,defaultValue="all") String collegeName,
//                          @RequestParam(value = "refresh", required = false,defaultValue="false") boolean refresh,
//                          @RequestParam(value = "name", required = true) String name) {
//        long now = System.currentTimeMillis();
//        if(!statumap.containsKey(name)){
//            statumap.put(name, new InnerStatu());
//        }
//        InnerStatu statu = statumap.get(name);
//        statu.crawlerSize = 0 ;
//        statu.crawlerStatus = 0;
////        crawlerStatus = 0;
////        crawlerSize = 0;
////        logSaver.clean();
//        statu.logclear();
//        log.info("/crawler " + refresh + " "  + organizationName + " " + collegeName + " " + name);
//
//        QueryParam param = new QueryParam();
//        if (!organizationName.equals("all")) {
//            param.addTerm(Term.build("organizationName", organizationName));
//        }
//        if (!collegeName.equals("all")) {
//            param.addTerm(Term.build("collegeName", collegeName));
//        }
//        if(!refresh){
//            param.addTerm(Term.build("handled", false));
//        }
//        param.addTerm(Term.build("writeBy", name));
//        // 读取已经爬取的数据，防止再次抓取重复
//        /*log.info("=============== Start reading finished scholars from scholar_temp! ===============");
//        List<ScholarTemp> temps = MongodbUtil.select(param, ScholarTemp.class);
//        log.info("=============== Reading process finished! Total num: " + temps.size() + ". ===============");
//        if (CollectionUtil.isNotEmpty(temps)) {
//            for (ScholarTemp temp : temps) {
//                ORG_NAME_URLS.add(temp.getOrganizationName() + "|" + temp.getCollegeName() + "|" + temp.getName());
//            }
//        }
//        temps.clear();*/
//
//        // 读取抓取高校院系规则，抓取学者名称、个人主页、职称
//        log.info("=============== Start loading configures from Database! ===============");
//        List<ScholarConfigure> configures = MongodbUtil.select(param, ScholarConfigure.class);
//        log.info("=============== Loading configures finished! Total count: " + configures.size() + " ===============");
//        /*List<ScholarConfigure> configures = new ArrayList<>();
//        ScholarConfigure configure1 = new ScholarConfigure("西安科技大学", "理学院", "", "教授", "https://skxy.qhnu.edu.cn/xygk/szdw.htm", "//*[@id=\"vsb_content_1117_u91\"]");
//        configures.add(configure1);*/
//        if (CollectionUtil.isEmpty(configures)) {
//            statu.crawlerStatus = 100;
////            crawlerStatus = 100;
//            return DateUtil.millisecondToTime(now);
//        }
//        int count = 0;
//        statu.crawlerSize = configures.size();
//        log.info("size =="+statu.crawlerSize);
//
//        for (ScholarConfigure configure : configures) {
//            count++;
//            statu.crawlerStatus = (long)(count/(double)statu.crawlerSize*100) - 1;
//
////            crawlerStatus = (int)(count/(double)crawlerSize*100) - 1;
//            log.info("=============== Python INFO: Detecting web charset ······ ===============");
//            String charset = CrawlerUtil.detectCharset(configure.getWebsite());
//            log.info("=============== Python INFO: Detecting process finished! Get web charset: {}. ===============", charset);
//            Spider.create(new ScholarSpider(configure, charset,name),name).setDownloader(new HttpsClientDownloader(name)).addUrl(configure.getWebsite()).thread(1).run();
//
//            Update update = new Update();
//            update.set("handled", true);
//            MongodbUtil.patch(configure.getId(), update, ScholarConfigure.class);
//            if (count % 10 == 0) {
//                log.info("=============== Total count: " + configures.size() + ". ===============");
//                log.info("=============== Finished num: " + count + ". ===============");
//            }
//        }
//
//        log.info("=============== Total count: " + configures.size() + ". Finished: " + count + " ===============");
//
//        count = 0;
//        Map<Integer,String> result = new HashMap<>();
//        ObjectMapper mapper = new ObjectMapper();
//        String resultString = "";
//        for(String str : statu.saver){
//            result.put(count++,str);
////            log.info(str);
//        }
//        try {
//            resultString = mapper.writeValueAsString(result);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
//        statu.crawlerStatus = 100;
////        crawlerStatus = 100;
//        return resultString;
//    }


//    int imgCrawlerStatus = 0;
//    int imgCrawlerSize = 0;
    @ApiOperation(value = "抓取学者状态")
    @GetMapping("/imgCrawler_status")
    public String imgCrawlerStatus(@RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        Map<String, Long> map = new HashMap<>();
        map.put("progress", statu.imgCrawlerStatus);
        map.put("size", statu.imgCrawlerSize);
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
                             @RequestParam(value = "refresh", required = false,defaultValue="false") boolean refresh,
                             @RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        statu.imgCrawlerSize = 0;
        statu.imgCrawlerStatus = 0;
//        imgCrawlerSize = 0;
//        imgCrawlerStatus = 0;
//        logSaver.clean();
        statu.logclear();
        log.info("/imgCrawler " + refresh + " "  + organizationName + " " + collegeName + " " + name);
        QueryParam param = new QueryParam();
        if (!organizationName.equals("all")) {
            param.addTerm(Term.build("organizationName", organizationName));
        }
        if (!collegeName.equals("all")) {
            param.addTerm(Term.build("collegeName", collegeName));
        }
        param.addTerm(Term.build("writeBy", name));

        log.info("=============== Start loading scholars from Database! ===============");
        List<ScholarConfigureTemp> configureTemps = MongodbUtil.select(param, ScholarConfigureTemp.class);
        log.info("=============== Loading scholars succeed! Total count: " + configureTemps.size() + ". ===============");

        List<ScholarConfigure> configures = new ArrayList<>();
        for (ScholarConfigureTemp temp : configureTemps) {
            ScholarConfigure configure = BeanCopierUtil.copy(temp, ScholarConfigure.class);
            configures.add(configure);
        }
        configureTemps.clear();
        MongodbUtil.delete(param,ScholarConfigureTemp.class);
        /*List<ScholarConfigure> configures = new ArrayList<>();
        ScholarConfigure configure1 = new ScholarConfigure("汕头大学", "商学院", "企业管理系", "教授", "http://swxy.tust.edu.cn/nr.aspx?id=831", "//*[@id='lm']");
        configures.add(configure1);*/

        int count = 0;
        statu.imgCrawlerSize = configures.size();
//        imgCrawlerSize = configures.size();

        for (ScholarConfigure configure : configures) {
//            set.add(configure.getOrganizationName() + " " + configure.getCollegeName());
            count++;
            statu.imgCrawlerStatus = (long)(count/(double)statu.imgCrawlerSize * 100 ) - 1;
//            imgCrawlerStatus = (int)(count/(double)imgCrawlerSize*100) - 1;
            log.info("=============== Python INFO: Detecting web charset ······ ===============");
            String charset = CrawlerUtil.detectCharset(configure.getWebsite());
            log.info("=============== Python INFO: Detecting process finished! Get web charset: {}. ===============", charset);
            Spider.create(new ScholarImgSpider(configure, charset,name),name).setDownloader(new HttpsClientDownloader(name)).addUrl(configure.getWebsite()).thread(1).run();

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
        for(String str : statu.saver){
            log.info(str);
            result.put(count++,str);
        }
        try {
            resultString = mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        statu.imgCrawlerStatus  = 100;
//        imgCrawlerStatus =  100;
        return resultString;
//        return "success";
    }
    @ApiOperation(value = "测试")
    @GetMapping("/testId")
    public String testId() {
//        Update update = new Update();
//        update.set("scholarId", "12345678");
//        MongodbUtil.patch("645bfcebb7274352acb4e6f9041b3e2f", update, ScholarTemp.class);
        return "test";
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


//    int detailStatus = 0;
//    int detailSize = 0;
    @ApiOperation(value = "抓取学者详情状态")
    @GetMapping("/detail_status")
    public String detailStatus(@RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        Map<String, Long> map = new HashMap<>();
        map.put("progress", statu.detailStatus);
        map.put("size", statu.detailSize);
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
            @RequestParam(value = "refresh", required = false,defaultValue="false") boolean refresh,
            @RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        statu.detailSize = 0;
        statu.detailStatus = 0;
//        detailStatus = 0;
//        detailSize = 0;
        log.info("/detail " + refresh + " " + organizationName + " " + collegeName + " " + name);
        //
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
        param.addTerm(Term.build("writeBy", name));
        log.info("=============== Start loading scholars from Database! ===============");
        if(!refresh){
            param.addTerm(Term.build("content", TermTypeEnum.NULL, null));
        }
        List<ScholarTemp> scholars = MongodbUtil.select(param, ScholarTemp.class);

        log.info("=============== Loading scholars succeed! Total count: " + scholars.size() + ". ===============");
//        detailSize = scholars.size();
        statu.detailSize = scholars.size();
        int count = 0;
        Map<Integer,String> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        if (refresh) {
            for (ScholarTemp scholar : scholars) {
                count++;
                if (StringUtil.isEmpty(scholar.getWebsite())) {
                    continue;
                }
                result.put(count, scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName());
                executor.execute(new ScholarSpiderRunnable(scholar,name));
                //Spider.create(new ScholarDetailSpider(scholar, CrawlerUtil.detectCharset(scholar.getWebsite()))).addUrl(scholar.getWebsite()).run();
            }
        } else {
            for (ScholarTemp scholar : scholars) {
                count++;
                if (StringUtil.isEmpty(scholar.getWebsite())) {
                    continue;
                }
                result.put(count, scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName());
                executor.execute(new ScholarSpiderRunnable(scholar,name));

                //Spider.create(new ScholarDetailSpider(scholar, CrawlerUtil.detectCharset(scholar.getWebsite()))).addUrl(scholar.getWebsite()).run();
            }
        }
        //
//        statu.detailStatus = 50;
        while(executor.getQueue().size() > 0){
//            detailStatus = (int)(100 - executor.getQueue().size()/(double)detailSize*100);
            if(statu.detailStatus < 90){
                statu.detailStatus++;
            }
//            statu.detailStatus = (long)(100 - executor.getQueue().size()/(double)statu.detailSize*100);
//            continue;
        }
        try {
            resultString = mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
//        detailStatus  = 100;
        statu.detailStatus = 100;
        return resultString;
    }

//    int detailMatchStatus = 0;
//    int detailMatchSize = 0;
    @ApiOperation(value = "抓取学者详情状态")
    @GetMapping("/detail_match_status")
    public String detailMatchStatus(@RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        Map<String, Long> map = new HashMap<>();
        map.put("progress", statu.detailMatchStatus);
        map.put("size", statu.detailMatchSize);
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
            @RequestParam(value = "refresh", required = false,defaultValue="false") boolean refresh,
            @RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
//        detailMatchStatus = 0;
//        detailMatchSize = 0;
        statu.detailMatchStatus = 0;
        statu.detailMatchSize = 0;
        log.info("/detail_match " + refresh + " " + organizationName + " " + collegeName + " " + name );
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
        param.addTerm(Term.build("writeBy", name));
        log.info("=============== Start loading scholars from Database! ===============");
        List<ScholarTemp> scholars = MongodbUtil.select(param, ScholarTemp.class);
        log.info("=============== Loading scholars succeed! Total count: " + scholars.size() + ". ===============");
//        detailMatchSize  = scholars.size();
        statu.detailMatchSize = scholars.size();
        int count = 0;
        if (refresh) {
            for (ScholarTemp scholar : scholars) {
                count++;
                Update update = new Update();
                update.set("match", false);
                MongodbUtil.patch(scholar.getId(), update, ScholarTemp.class);
                if (StringUtil.isEmpty(scholar.getContent()) || "null".equals(scholar.getContent())) {
                    continue;
                }
                executor.execute(new ScholarTempRunnable(scholar));
            }
        } else {
            for (ScholarTemp scholar : scholars) {
                count++;
                if (StringUtil.isEmpty(scholar.getContent()) || "null".equals(scholar.getContent())) {
                    continue;
                }
//                statu.set.add(scholar.getOrganizationName() + " " + scholar.getCollegeName());
                executor.execute(new ScholarTempRunnable(scholar));
            }

        }
//        statu.detailMatchStatus = 50;
        while(executor.getQueue().size() > 0){
//            statu.detailMatchStatus  = (long)(100 - executor.getQueue().size()/(double) statu.detailMatchSize*100);
////            detailMatchStatus = (int)(100 - executor.getQueue().size()/(double)detailMatchSize*100);
//            System.out.println("test " + executor.getQueue().size());
            if(statu.detailMatchStatus < 90){
                statu.detailMatchStatus ++;
            }
        }
        if(refresh == false){
            scholars = new ArrayList<ScholarTemp>();
            for(String s : statu.set){
                System.out.println(s);
                param = new QueryParam();
                String[] split =  s.split(" ");
                param.addTerm(Term.build("organizationName", split[0]));
                try{
                    param.addTerm(Term.build("collegeName", split[1]));
                }
                catch (Exception e){
                    ;
                }
                param.addTerm(Term.build("writeBy", name));


                scholars.addAll( MongodbUtil.select(param, ScholarTemp.class));
            }
            log.info("=============== Loading scholars succeed! Total count: " + scholars.size() + ". ===============");
            statu.detailMatchSize = scholars.size();
//            System.out.println("*****" + scholars.size());
//           detailMatchSize  = scholars.size();
            statu.set.clear();
        }else{
            statu.detailMatchSize = scholars.size();
//            detailMatchSize  = scholars.size();
            scholars = MongodbUtil.select(param, ScholarTemp.class);
        }
        count = 0;
        Map<Integer,String> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";

        String getUrl="http://47.92.240.36/academic-apply/apply/scholar/getScholarByInfo";
        List<ScholarMultiId> multilist = new ArrayList<>();
        for (ScholarTemp scholar : scholars) {
            count++;

            if(StringUtil.isEmpty(scholar.getScholarId()) || "null".equals(scholar.getScholarId())){
                //            httpget设置
                Map<String,Object> map=new HashMap<String,Object>();
                map.put("name", HttpRequest.encodeParam(scholar.getName()));
                String orgName = scholar.getOrganizationName();
                if(!(StringUtil.isEmpty(orgName) || "null".equals(orgName))){
                    map.put("orgName",HttpRequest.encodeParam(orgName));

                }
                String department =  scholar.getCollegeName();
                if(!(StringUtil.isEmpty(department) || "null".equals(department))){
                    map.put("department", HttpRequest.encodeParam(department));
                }
                log.info(scholar.getName() + " " + orgName + " "+ department );
//
                String json = HttpRequest.sendGet(getUrl, map,"utf-8");
                int size = (int) JSONPath.read(json,"$.data.size()");
                if(size > 1){
                    StringBuffer ids = new StringBuffer();
                    for(int i = 0 ; i < size ; i ++){
//                    System.out.println(JSONPath.read(json,String.format("$.data[%d]",i)));
                        ids.append(JSONPath.read(json,String.format("$.data[%d].id",i)).toString());
                        ids.append(" ");
//                    System.out.println(JSONPath.read(json,String.format("$.data[%d].id",i)));
                    }
                    multilist.add(new ScholarMultiId(scholar,json,ids.toString(),name));
//                存信息到新数据库等待处理
                }else if(size == 1){
                    System.out.println(JSONPath.read(json,"$.data[0].id") + " " + scholar.getName() + " " + orgName + " "+ department);
                    String scholarId = JSONPath.read(json,"$.data[0].id").toString();
                    scholar.setScholarId(scholarId);
                    Update update = new Update();
                    update.set("scholarId", scholarId);
                    MongodbUtil.patch(scholar.getId(), update, ScholarTemp.class);
                }
            }
            statu.scholars.add(scholar);
            result.put(count, scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName() + " " + scholar.getTitle() + " " + scholar.getEmail() + " " + scholar.getPhone() + " " + scholar.getId() + " " + scholar.getScholarId());

        }
        if(multilist.size() != 0){
            MongodbUtil.save(multilist);
        }

        try {
            resultString = mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
//        System.out.println(resultString);
//        detailMatchStatus = 100;
        statu.detailMatchStatus = 100;
        return resultString;
    }

    @ApiOperation(value = "确认入库")
    @GetMapping("/saveTozhitu")
    public String saveTozhitu(@RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        int count = statu.scholars.size();
//确保与库中一致
        for(ScholarTemp scholar : statu.scholars){
            QueryParam param = new QueryParam();
            param.addTerm(Term.build("id", scholar.getId()));
            List<ScholarTemp> temp = MongodbUtil.select(param, ScholarTemp.class);
//            System.out.println(temp.size());
            for(ScholarTemp temps : temp){
//                System.out.println(temps.getId() + temps.getName());
                scholar.setName(temps.getName());
                scholar.setOrganizationName(temps.getOrganizationName());
                scholar.setCollegeName(temps.getCollegeName());
                scholar.setTitle(temps.getTitle());
                scholar.setEmail(temps.getEmail());
                scholar.setPhone(temps.getPhone());
            }
        }




        for(ScholarTemp scholar : statu.scholars){
            String putUrl = "http://47.92.240.36/academic-apply/apply/scholar/updateScholarBasic";
            Map<String,Object> map=new HashMap<String,Object>();

            map.put("id", scholar.getScholarId());
            map.put("name",HttpRequest.encodeParam(scholar.getName()));
            map.put("orgName",HttpRequest.encodeParam(scholar.getOrganizationName()));

            if(!(StringUtil.isEmpty(scholar.getCollegeName()) || "null".equals(scholar.getCollegeName()))){
                map.put("department",HttpRequest.encodeParam(scholar.getCollegeName()));
            }
            if(!(StringUtil.isEmpty(scholar.getEmail()) || "null".equals(scholar.getEmail()))){
                map.put("email",scholar.getEmail());
            }
            if(!(StringUtil.isEmpty(scholar.getTitle()) || "null".equals(scholar.getTitle()))){
                map.put("title",HttpRequest.encodeParam(scholar.getTitle()));
            }
            if(!(StringUtil.isEmpty(scholar.getPhone()) || "null".equals(scholar.getPhone()))){
                map.put("phone",scholar.getPhone());
            }
            if(!(StringUtil.isEmpty(scholar.getWebsite()) || "null".equals(scholar.getWebsite()))){
                map.put("url",scholar.getWebsite());
            }
            String json = HttpRequest.sendPut(putUrl, map,"utf-8");
        }
        statu.scholars.clear();
        return ""+count;
    }

    @ApiOperation(value = "错误信息接口")
    @GetMapping("/errors")
    public String errors(@RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        Map<Integer, String> map = new HashMap<>();
        int count = 1;
        for(String s : statu.errsaver){
            map.put(count++,s);
        }
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        try {
            resultString = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
    }

    @ApiOperation(value = "错误日志")
    @GetMapping("/errorlog")
    public String errorlog(@RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        Map<Integer, String> map = new HashMap<>();
        int count = 1;
        for(String s : statu.errlog){
            map.put(count++,s);
        }
        ObjectMapper mapper = new ObjectMapper();
        String resultString = "";
        try {
            resultString = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return resultString;
    }


    @Autowired
    private SensitiveFilter sensitiveFilter;
//    int antiCrawlerStatus = 0;
//    int antiCrawlerSize = 0;
    @ApiOperation(value = "反爬虫详情状态")
    @GetMapping("/anti_crawler_status")
    public String antiCrawlerStatus(@RequestParam(value = "name", required = true) String name) {
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        Map<String, Long> map = new HashMap<>();
        map.put("progress", statu.antiCrawlerStatus);
        map.put("size", statu.antiCrawlerSize);
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
            @RequestParam(value = "collegeName", required = false,defaultValue="all") String collegeName,
            @RequestParam(value = "name", required = true) String name) {
        long now = System.currentTimeMillis();
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        statu.antiCrawlerSize = 0 ;
        statu.antiCrawlerStatus = 0;
//        antiCrawlerStatus = 0;
//        antiCrawlerSize = 0;
        log.info("/anti_crawler " + organizationName + " " + collegeName + " " + name);
        QueryParam param = new QueryParam();
        if (!organizationName.equals("all")) {
            param.addTerm(Term.build("organizationName", organizationName));
        }
        if (!collegeName.equals("all")) {
            param.addTerm(Term.build("collegeName", collegeName));
        }
        param.addTerm(Term.build("writeBy", name));
        log.info("=============== Start loading scholars from Database! ===============");
        List<ScholarTemp> scholars = MongodbUtil.select(param, ScholarTemp.class);
        log.info("=============== Loading scholars succeed! Total count: " + scholars.size() + ". ===============");
//        antiCrawlerSize = scholars.size();
        statu.antiCrawlerSize = scholars.size();
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
                statu.antiCrawlerStatus = (long)(count/(double)statu.antiCrawlerSize*100);
//                antiCrawlerStatus  = (int)(count/(double)antiCrawlerSize*100);
                result.put(count++, scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName());
//                log.info("重新爬取" + scholar.getOrganizationName() + " " +scholar.getCollegeName() + " " + scholar.getName());
//               Spider.create(new ScholarDetailSpider(scholar, CrawlerUtil.detectCharset(scholar.getWebsite()))).setDownloader(new HttpsClientDownloader(name)).addUrl(scholar.getWebsite()).run();
                Spider.create(new ScholarDetailSpider(scholar, CrawlerUtil.detectCharset(scholar.getWebsite())),name).setDownloader(new HttpsClientDownloader(name)).addUrl(scholar.getWebsite()).run();
            }
        }
        statu.antiCrawlerSize = result.size();
//        antiCrawlerSize = result.size();

//        antiCrawlerStatus  = 100;
        try {
            resultString = mapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        statu.antiCrawlerStatus = 100;
        return resultString;
    }

    @GetMapping("/update_status")
    public void updateStatus(@RequestParam(value = "name", required = true) String name){
        if(!statumap.containsKey(name)){
            statumap.put(name, new InnerStatu());
        }
        InnerStatu statu = statumap.get(name);
        statu.clear();

//        logSaver.clean();
//        antiCrawlerStatus = 0;
//        antiCrawlerSize = 0;
//        detailMatchStatus = 0;
//        detailMatchSize = 0;
//        detailStatus = 0;
//        detailSize = 0;
//        imgCrawlerStatus = 0;
//        imgCrawlerSize = 0;
//        crawlerStatus = 0;
//        crawlerSize = 0;
//        loadConfigStatus = 0;
//        loadConfigSize = 0;
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
            Spider.create(new ScholarDetailSpider(temp, temp.getWebsite()),"").addUrl(temp.getWebsite()).run();
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
        String name;
        public ScholarSpiderRunnable(ScholarTemp scholar,String name) {
            this.scholar = scholar;
            this.name = name;
        }

        @Override
        public void run() {
            Spider.create(new ScholarDetailSpider(scholar, CrawlerUtil.detectCharset(scholar.getWebsite())),name).setDownloader(new HttpsClientDownloader(name)).addUrl(scholar.getWebsite()).run();
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
