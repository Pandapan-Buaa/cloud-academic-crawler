package com.sheep.cloud.academic.crawler.runnable;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.sheep.cloud.academic.crawler.entity.ScholarTemp;
import com.sheep.cloud.core.entity.AbstractRunnable;
import com.sheep.cloud.core.util.DateUtil;
import com.sheep.cloud.core.util.StringUtil;
import com.sheep.cloud.open.mongodb.util.MongodbUtil;
import com.sheep.cloud.open.redis.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sheep.cloud.academic.crawler.constants.AcademicConstant.SCHOLAR_TITLES;
import static com.sheep.cloud.core.constants.PatternConstant.EMAIL;
import static com.sheep.cloud.core.constants.PatternConstant.MOBILE;

/**
 * @author Yang
 * @create 2019-03-06 11:15
 * <p>
 * Modified Fan zk
 **/
@Slf4j
public class ScholarTempRunnable implements Runnable {

    ScholarTemp scholar;

    public ScholarTempRunnable(ScholarTemp scholar) {
        this.scholar = scholar;
    }

    @Override
    public void run() {
        try {
            String content;
            String title;
            String email;
            String phone;

            content = scholar.getContent();
            title = matchTitle(content);
            email = matchEmail(content);
            phone = matchPhone(content);

            Update update = new Update();
            boolean flag = false;
//            log.info(title + " " + email + " " + phone );
            if ((StringUtil.isEmpty(scholar.getTitle()) || "null".equals(scholar.getTitle())) && StringUtil.isNotEmpty(title)) {
                flag = true;
                update.set("title", title);
            }
            if ((StringUtil.isEmpty(scholar.getEmail()) || "null".equals(scholar.getEmail())) && StringUtil.isNotEmpty(email)) {
                flag = true;
                update.set("email", email);
            }
            if ((StringUtil.isEmpty(scholar.getPhone()) || "null".equals(scholar.getPhone())) && StringUtil.isNotEmpty(phone)) {
                flag = true;
                update.set("phone", phone);
            }
            if (flag) {
                update.set("match", true);
                log.info("================ update:{}", JSON.toJSONString(update));
                MongodbUtil.patch(scholar.getId(), update, ScholarTemp.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String matchTitle(String content) {
        String result = "";
        try {
            Matcher matcher = Pattern.compile("职称：(院士|长聘教轨副教授|长聘副教授|长聘教授|助理教授|副教授|教授|讲师|高级工程师|高级实验师|助理实验师|工程师|实验师|副研究员|助理研究员|研究员|助教|高工)+").matcher(content);
            while (matcher.find()) {
                result = matcher.group().replaceAll("职称：", "");
                break;
            }
            if (StringUtil.isEmpty(result) && content.contains("职称")) {
                if (content.contains("职称：")) {
                    content = content.substring(content.indexOf("职称："));
                } else if (content.contains("职称:")) {
                    content = content.substring(content.indexOf("职称:"));
                } else {
                    content = content.substring(content.indexOf("职称"));
                }
            }
            if (StringUtil.isEmpty(result)) {
                for (String title : SCHOLAR_TITLES) {
                    if (content.contains(title)) {
                        result = title;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public String matchEmail(String content) {
        String result = "";
        try {
            if (StringUtil.containsIgnoreCase(content, "mail")) {
                content = content.substring(StringUtil.indexOfIgnoreCase(content, "mail"));
            } else if (content.contains("邮件")) {
                content = content.substring(content.indexOf("邮件"));
            } else if (content.contains("邮箱")) {
                content = content.substring(content.indexOf("邮箱"));
            } else if (content.contains("信箱")) {
                content = content.substring(content.indexOf("信箱"));
            }
            Matcher matcher = EMAIL.matcher(content);
            while (matcher.find()) {
                result = matcher.group();
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public String matchPhone(String content) {
        String result = "";
        try {
            if (content.contains("联系方式")) {
                content = content.substring(content.indexOf("联系方式"));
            } else if (content.contains("电话")) {
                content = content.substring(content.indexOf("电话"));
            } else if (StringUtil.containsIgnoreCase(content, "tel")) {
                int index = StringUtil.indexOfIgnoreCase(content, "tel");
                content = content.substring(index);
            } else if (StringUtil.containsIgnoreCase(content, "phone")) {
                int index = StringUtil.indexOfIgnoreCase(content, "phone");
                content = content.substring(index);
            }
            Matcher matcher = MOBILE.matcher(content);
            while (matcher.find()) {
                result = matcher.group();
                break;
            }
            if (StringUtil.isEmpty(result)) {
                String fixedLineTel = "0\\d{2,3}-[1-9]\\d{6,7}";
                Pattern tel = Pattern.compile(fixedLineTel);
                Matcher matcher1 = tel.matcher(content);
                while (matcher1.find()) {
                    result = matcher1.group();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}