package com.sheep.cloud.academic.crawler.util;

import com.google.common.collect.Maps;
import com.sheep.cloud.academic.crawler.entity.ScholarTemp;
import com.sheep.cloud.academic.crawler.webmagic.HttpsClientDownloader;
import com.sheep.cloud.core.util.CollectionUtil;
import com.sheep.cloud.core.util.HtmlUtil;
import com.sheep.cloud.core.util.StringUtil;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.SimpleProxyProvider;
import us.codecraft.webmagic.selector.Selectable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author YangChao
 * @create 2019-05-13 14:45
 **/
public class CrawlerUtil extends HtmlUtil {

    /**
     * 把标签形如<span class='class'>123</span> 替换为123
     *
     * @param selectable
     * @return
     * 正则表达式修正 Fan zk
     */
    public static String getText(Selectable selectable) {
        if (selectable == null || StringUtil.isEmpty(selectable.get())) {
            return "";
        }

        String ret;
        ret = selectable.get().replaceAll("<script[\\s\\S]*?</script>", "");
        ret = ret.replaceAll("<style[\\s\\S]*?</style>", "");
        ret = ret.replaceAll("<!--[\\s\\S]*?-->", "");
        ret = ret.replaceAll("<[\\s\\S]*?>", "");
        ret = ret.replaceAll("</[\\s\\S]*?>", "");

        return ret;
    }

    /**
     * 把标签形如<span class='class'>123</span> 替换为123
     *
     * @param selectable
     * @return
     */
    public static String getTextTrim(Selectable selectable) {
        String result = getText(selectable);
        return replace(result);
    }

    /**
     * 把标签形如<span class='class'>123</span> 替换为123
     *
     * @param tagNames
     * @return
     */
    public static String formatTextByTag(Selectable selectable, String... tagNames) {
        return formatTextByTag(selectable.get(), tagNames);
    }

    /**
     * 把标签形如<span class='class'>123</span> 替换为123
     *
     * @param tagNames
     * @return
     */
    public static String formatTextByTag(String nodeStr, String... tagNames) {
        if (StringUtil.isBlank(nodeStr)) {
            return "";
        }
        for (String tagName : tagNames) {
            nodeStr = nodeStr.replaceAll(StringUtil.format("<{}.*?>", tagName), "").replaceAll(StringUtil.format("</{}>", tagName), "");
        }
        return replace(nodeStr);
    }

    /**
     * 去掉非法空格以及指定字符串
     *
     * @param nodeStr
     * @param trimNames
     * @return
     */
    public static String replace(String nodeStr, String... trimNames) {
        if (StringUtil.isBlank(nodeStr)) {
            return "";
        }
        while (nodeStr.contains("\n\n")) {
            nodeStr = nodeStr.replace("\n\n", "\n");
        }
        nodeStr = nodeStr.replaceAll("\t+", "");
        nodeStr = nodeStr.replaceAll(" {2,}", " ");
        nodeStr = replaceBlank(nodeStr);
        nodeStr = nodeStr.replaceAll("<br>", "");
        nodeStr = nodeStr.replaceAll("&nbsp;", "");
        nodeStr = nodeStr.replaceAll("&amp;nbsp", "");
        nodeStr = nodeStr.replaceAll("&amp;", "");
        nodeStr = nodeStr.replaceAll("&amp", "");
        nodeStr = nodeStr.replaceAll("&gt;", "");
        nodeStr = nodeStr.replaceAll(" ", "");
        nodeStr = nodeStr.replaceAll("　", "");
        nodeStr = nodeStr.replaceAll(" ", "");
        for (String trimName : trimNames) {
            nodeStr = nodeStr.trim().replaceAll(trimName, "");
        }
        return nodeStr.trim();
    }

    private static boolean isLetter(char c) {
        return Character.isLowerCase(c) || Character.isUpperCase(c);
    }

    /**
     * 去掉中文间的空格但不去掉英文中间的空格
     *
     * @param nodeStr
     * @return ret
     * @author Fanzk
     */

    private static String replaceBlank(String nodeStr) {
        StringBuilder ret = new StringBuilder();
        if (!StringUtil.isBlank(nodeStr)) {
            List<Integer> flagList = new ArrayList<>();
            char[] chars = nodeStr.toCharArray();
            for (int i = 1; i < chars.length - 1; i++) {
                if (chars[i] == ' ') {
                    if (isLetter(chars[i - 1]) || isLetter(chars[i + 1])) {
                        continue;
                    } else {
                        flagList.add(i);
                    }
                }
            }
            if (chars[0] != ' ') {
                ret.append(chars[0]);
            }
            for (int i = 1; i < chars.length; i++) {
                if (!flagList.contains(i)) {
                    ret.append(chars[i]);
                }
            }
        }
        return ret.toString();
    }

    /**
     * 截取报文体
     *
     * @param url
     * @return
     */
    public static Map<String, String> getParam(String url) {
        Map<String, String> result = Maps.newHashMap();
        if (StringUtil.isEmpty(url)) {
            return result;
        }
        String paramStr = StringUtil.subAfter(url, '?', false);
        List<String> paramList = StringUtil.split(paramStr, '&');
        if (CollectionUtil.isEmpty(paramList)) {
            return result;
        }
        for (String param : paramList) {
            result.put(StringUtil.subBefore(param, "=", true).trim(), StringUtil.subAfter(param, "=", true));
        }
        return result;
    }

    public static HttpClientDownloader getProxy() {
        // 代理服务器
        String proxyHost = "http-dyn.abuyun.com";
        int proxyPort = 9020;

        // 代理隧道验证信息
        String proxyUser = "HX59410BP8D5878D";
        String proxyPass = "6A2B9E88C34FD95A";

        HttpClientDownloader httpClientDownloader = new HttpClientDownloader();
        httpClientDownloader.setProxyProvider(SimpleProxyProvider.from(new Proxy(proxyHost, proxyPort, proxyUser, proxyPass)));
        return httpClientDownloader;
    }

    public static HttpsClientDownloader getProxys() {
        // 代理服务器
        String proxyHost = "http-dyn.abuyun.com";
        int proxyPort = 9020;

        // 代理隧道验证信息
        String proxyUser = "HX59410BP8D5878D";
        String proxyPass = "6A2B9E88C34FD95A";

        HttpsClientDownloader httpClientDownloader = new HttpsClientDownloader();
        httpClientDownloader.setProxyProvider(SimpleProxyProvider.from(new Proxy(proxyHost, proxyPort, proxyUser, proxyPass)));
        return httpClientDownloader;
    }

    /**
     * @author FanZk
     * 除去详情页中多余的边栏信息及代码信息
     */

    public static String simplifyContent(String content, ScholarTemp scholar) {
        StringBuilder ret = new StringBuilder();

        //按行分开检测
        if (content.contains("\n") || content.contains("\r")) {
            String[] items = content.split("[\n|\r]+");
            /*
             * 这段代码用于检测合适的个人详情信息开头句，以flag标记
             * 字符串长度限定为10可适当应情况更改
             * 是否找到第一个就break的问题也可视情况调整
             */
            List<Integer> flagList = new ArrayList<>();
            final int LEN_LIMIT = 100;
            for (int i = 1; i < items.length; i++) {
                if (items[i].contains(scholar.getName())) {
                    if (!items[i].contains(scholar.getOrganizationName())) {
                        if (StringUtil.isEmpty(scholar.getCollegeName())) {
                            if (items[i].length() < LEN_LIMIT) {
                                flagList.add(i);
                                break;
                            }
                        } else {
                            if (!items[i].contains(scholar.getCollegeName()) && items[i].length() < LEN_LIMIT) {
                                flagList.add(i);
                            }
                        }
                    }
                }
            }
            int flag = 0;
            if (!flagList.isEmpty()) {
                flag = flagList.get(flagList.size() - 1);
            } else {
                for (int i = 1; i < items.length; i++) {
                    if (items[i].contains(scholar.getName())) {
                        flag = i;
                        break;
                    }
                }
            }
//            int flag = 0;
//            for (int i = 0; i < items.length; i++) {
//                if (items[i].contains("管理学博士")) {
//                    flag = i;
//                    break;
//                }
//            }
            for (int i = flag; i < items.length; i++) {
                String line = items[i];
                if (StringUtil.isEmpty(line)) {
                    continue;
                }
                if (line.contains("《") || line.contains("》")) {
                    ret.append(line).append('\n');
                    continue;
                }
                if (StringUtil.containsIgnoreCase(line, "copyright") || line.contains("版权所有") || line.contains("©") || line.contains("(c)") || line.contains("(C)")) {
                    break;
                }
                if (line.equals("作者") || line.equals("次") || line.equals("作者：") || line.equals("作者 ：") || line.equals("作者： ") || line.equals("作者 ： ")) {
                    continue;
                }
                if (line.contains("阅读次数") || line.contains("点击") || line.contains("发布时间") || line.contains("发布日期") || line.contains("创建时间") || line.contains("创建日期") || line.contains("来源") || line.contains("浏览") || line.contains("登记时间") || line.contains("登记日期") || line.contains("上传时间") || line.contains("上传日期") || line.contains("访问量") || line.contains("更新时间") || line.contains("访问人数")) {
                    continue;
                }
                if (line.contains("字体") || line.contains("字号") || line.contains("[大]") || line.contains("[中]") || line.contains("[小]") || line.contains("【大】") || line.contains("【中】") || line.contains("【小】")) {
                    continue;
                }
                if (line.equals("[") || line.equals("]") || line.equals("【") || line.equals("】")) {
                    continue;
                }
                if (line.equals("大") || line.equals("中") || line.equals("小") || line.equals("[大") || line.equals("大]") || line.equals("[小") || line.equals("小]")) {
                    continue;
                }
                if (line.contains("名师讲学") || line.contains("学院指南") || line.contains("学院概况") || line.contains("学校指南")) {
                    break;
                }
                if (line.contains("友情链接") || line.contains("其他链接") || line.contains("相关链接") || line.contains("推荐链接") || line.contains("本地链接") || line.contains("校内链接")) {
                    break;
                }
                if (line.contains("转发") || line.contains("分享") || line.contains("新浪") || line.contains("腾讯")) {
                    break;
                }
                if (line.contains("上一篇") || line.contains("上一页") || line.contains("上一条") || line.contains("上一个") || line.contains("上一图集")) {
                    break;
                }
                if (line.contains("下一篇") || line.contains("下一页") || line.contains("下一条") || line.contains("下一个") || line.contains("下一图集")) {
                    break;
                }
                if (line.contains("学校主页") || line.contains("首页") || line.contains("学校网站") || line.contains("校内网站")) {
                    break;
                }
                if (line.contains("联系我们") || line.contains("校友") || line.contains("旧版网站") || line.contains("关于我们")) {
                    break;
                }
                if (line.contains("留言") || line.contains("下载")) {
                    break;
                }
                if (line.contains("关闭") || (line.contains("打印") && !line.contains("3D打印") && !line.contains("打印技术"))) {
                    continue;
                }
                if (line.contains("公众号") || line.contains("官方微信") || line.contains("微博")) {
                    continue;
                }
                if (line.contains("手机版") || line.contains("扫一扫")) {
                    continue;
                }
                if (line.contains("技术团队") || line.contains("所在科室") || line.contains("出诊安排")) {
                    break;
                }
                if (line.contains("名人作品") || line.contains("相关专家") || line.contains("其他专家")) {
                    break;
                }
                if (line.contains("发布单位") || line.contains("已阅览") || line.contains("已浏览") || line.contains("日期")) {
                    continue;
                }
                if (line.contains("附件") || line.contains("返回顶部") || line.contains("点赞")) {
                    break;
                }
                ret.append(line).append('\n');
            }
        } else {
            ret.append(content);
        }

        return ret.toString();
    }

    /**
     * @author FanZk
     * 网站编码探测（防止乱码）、Python识别人名脚本
     */

    public static String detectCharset(String url) {
        String ret = "";
        try {
            String[] args = new String[]{"python", "E:\\cloud-academic-crawler\\src\\main\\java\\com\\sheep\\cloud\\academic\\crawler\\util\\detectCharset.py", String.valueOf(url)};
            Process proc = Runtime.getRuntime().exec(args);
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            ret = in.readLine();
            in.close();
            proc.destroy();
            assert ret != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<String> startPythonShellToGetAllName(String text) {
        List<String> list = new ArrayList<>();
        String ret = "";
        try {
            String[] args = new String[]{"python", "E:\\cloud-academic-crawler\\src\\main\\java\\com\\sheep\\cloud\\academic\\crawler\\util\\getAllNameLTP.py", String.valueOf(text)};
            Process proc = Runtime.getRuntime().exec(args);
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            ret = in.readLine();
            in.close();
            proc.destroy();
            assert ret != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!ret.equals(" ")) {
            String[] names = ret.split(" ");
            Collections.addAll(list, names);
        }
        return list;
    }

    public static String startPythonShellToGetName(String text) {
        String ret = "";
        try {
            String[] args1 = new String[]{"python", "E:\\cloud-academic-crawler\\src\\main\\java\\com\\sheep\\cloud\\academic\\crawler\\util\\getNameLTP.py", String.valueOf(text)};
            Process proc1 = Runtime.getRuntime().exec(args1);
            BufferedReader in1 = new BufferedReader(new InputStreamReader(proc1.getInputStream()));
            ret = in1.readLine();
            in1.close();
            proc1.destroy();
            assert ret != null;
            if (ret.equals(" ") || ret.length() == 1) {
                String[] args2 = new String[]{"python", "E:\\cloud-academic-crawler\\src\\main\\java\\com\\sheep\\cloud\\academic\\crawler\\util\\getNameHanlp.py", String.valueOf(text)};
                Process proc2 = Runtime.getRuntime().exec(args2);
                BufferedReader in2 = new BufferedReader(new InputStreamReader(proc2.getInputStream()));
                ret = in2.readLine();
                in2.close();
                proc2.destroy();
                assert ret != null;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (ret.equals(" ")) {
            return "None";
        } else {
            return ret;
        }
    }

}