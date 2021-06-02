package com.sheep.cloud.academic.crawler.entity;

import com.sheep.cloud.core.entity.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.beetl.sql.core.annotatoin.AssignID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author YangChao
 * @create 2019-03-13 16:28
 **/
@Document(collection = "scholar_configure")
@Data
@NoArgsConstructor
public class ScholarConfigure extends BaseEntity {

    @Id
    private String id;

    @ApiModelProperty(name = "大学名称")
    private String organizationName;

    @ApiModelProperty(name = "院名称")
    private String collegeName;

    @ApiModelProperty(name = "系名称")
    private String departmentName;

    @ApiModelProperty(name = "职称 讲师、副教授、教授")
    private String title;

    @ApiModelProperty(name = "网址")
    private String website;

    @ApiModelProperty(name = "节点")
    private String xpath;

    @ApiModelProperty(name = "是否已抓取")
    private boolean handled;

    @ApiModelProperty(name = "录入人")
    private String writeBy;

    public ScholarConfigure(String organizationName, String collegeName, String departmentName, String title, String website, String xpath) {
        this.organizationName = organizationName;
        this.collegeName = collegeName;
        this.departmentName = departmentName;
        this.title = title;
        this.website = website;
        this.xpath = xpath;
        this.handled = false;
    }

}