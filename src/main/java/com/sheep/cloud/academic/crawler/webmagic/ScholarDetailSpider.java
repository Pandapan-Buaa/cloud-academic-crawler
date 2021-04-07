package com.sheep.cloud.academic.crawler.webmagic;

import com.sheep.cloud.academic.crawler.entity.ScholarTemp;
import com.sheep.cloud.academic.crawler.util.CrawlerUtil;
import com.sheep.cloud.core.util.StringUtil;
import com.sheep.cloud.open.mongodb.util.MongodbUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Update;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

/**
 * @author YangChao
 * @create 2019-05-05 15:31
 **/
@Slf4j
public class ScholarDetailSpider implements PageProcessor {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setTimeOut(30000);

    private ScholarTemp scholar;
    private String charset;

    public ScholarDetailSpider(ScholarTemp scholar, String charset) {
        this.scholar = scholar;
        this.charset = charset;
    }

    @Override
    public void process(Page page) {
        Html html = page.getHtml();

        String content = dealContent(html);
        if (StringUtil.isEmpty(content)) {
            return;
        }
        if (StringUtil.isNotEmpty(content)) {
            Update update = new Update();
            update.set("content", content);
            update.set("mainPage", true);
            MongodbUtil.patch(scholar.getId(), update, ScholarTemp.class);
            //log.info("patch ======================================== content:{}", content);
        }
    }

    private String dealContent(Selectable selectable) {
        String result = CrawlerUtil.getTextTrim(selectable);
        if (result.contains("系统提示") && result.contains("页面") && (result.contains("不存在") || result.contains("迁移") || result.contains("出错") || result.contains("无法访问"))) {
            return null;
        } else {
            return CrawlerUtil.simplifyContent(result, scholar);
        }
    }

    @Override
    public Site getSite() {
        return site.setCharset(this.charset).addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.75 Safari/537.36");
    }

}