package com.sheep.cloud.academic.crawler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.engine.beetl.BeetlEngine;
import com.google.common.collect.Maps;
import com.sheep.cloud.core.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.beetl.sql.core.kit.GenKit;
import org.beetl.sql.ext.gen.SourceGen;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author YangChao
 * @create 2019-03-20 15:51
 **/
@Slf4j
public class GeneratorTest {

    @Test
    public void generatorTest() {
        //CodeGeneratorUtil.generator("jdbc:mysql://47.96.98.189/cloud-basic-data?useUnicode=true&serverTimezone=UTC&useSSL=false","root","Assoda1314","com.sheep.cloud.basic.data" );
      /* String url = "http://www.me.tsinghua.edu.cn/column/25_1.html";
        log.info(url.substring(0, url.indexOf("/", 10)));*/
        String value = "职称：教授 12313";
        Matcher m = Pattern.compile("职称：[院士|长聘教轨副教授|长聘副教授|长聘教授|助理教授|副教授|教授|讲师|高级工程师|高级实验师|助理实验师|工程师|实验师|副研究员|助理研究员|研究员|助教|高工]+").matcher(value);
        while(m.find()) {
            log.info("Match number "+m.group(0));
        }
    }

    @Test
    public void generator() {
        String packagePath = "com.sheep.cloud.academic.crawler";
        String className = "ScholarTemp";
        String type = "AbstractMongodbCrudDao"; //AbstractMongodbCrudDao AbstractBeetlCrudDao

        BeetlEngine template1 = new BeetlEngine(new TemplateConfig("template", TemplateConfig.ResourceMode.CLASSPATH));
        Map<String, Template> templateMap = Maps.newHashMap();
        //templateMap.put("Entity", template1.getTemplate("Entity.java.btl"));
        templateMap.put("EntityVO", template1.getTemplate("EntityVO.java.btl"));
        templateMap.put("Dao", template1.getTemplate("Dao.java.btl"));
        templateMap.put("Service", template1.getTemplate("Service.java.btl"));
        templateMap.put("ServiceImpl", template1.getTemplate("ServiceImpl.java.btl"));
        templateMap.put("Controller", template1.getTemplate("Controller.java.btl"));

        Dict dict = Dict.create();
        dict.set("package", packagePath);
        dict.set("pathName", StringUtil.toSymbolCase(className, '-'));
        dict.set("className", className);
        dict.set("author", "YangChao");
        dict.set("datetime", DateUtil.now());
        dict.set("type", type);
        templateMap.keySet().forEach((t) -> {
            Template template = templateMap.get(t);
            String content = template.render(dict);
            try {
                String packagePathTemp = packagePath + "." + t.toLowerCase().replaceAll("entityvo", "VO").replaceAll("serviceimpl", "service");
                SourceGen.saveSourceFile(GenKit.getJavaSRCPath(), packagePathTemp, (className + t).replaceAll("Entity", ""), content);
            } catch (IOException var8) {
                var8.printStackTrace();
            }
        });
    }

}