package com.sheep.cloud.academic.crawler.entity;

import com.sheep.cloud.core.entity.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author YangChao
 * @create 2019-03-13 17:06
 **/
@Document(collection = "scholar")
@Data
@NoArgsConstructor
public class Scholar extends BaseEntity {

    @Id
    private String id;

    @ApiModelProperty(name = "大学名称")
    private String organizationName;

    @ApiModelProperty(name = "院名称")
    private String collegeName;

    @ApiModelProperty(name = "系名称")
    private String departmentName;

    @ApiModelProperty(name = "名称")
    private String name;

    @ApiModelProperty(name = "性别")
    private String gender;

    @ApiModelProperty(name = "出生日期")
    private String birthDate;

    @ApiModelProperty(name = "学历")
    private String education;

    @ApiModelProperty(name = "职位 博导、硕导")
    private String positions;

    @ApiModelProperty(name = "职称 讲师、副教授、教授")
    private String title;

    @ApiModelProperty(name = "荣誉奖励 中国科学院院士、长江学者、杰出青年")
    private String awards;

    @ApiModelProperty(name = "邮件")
    private String email;

    @ApiModelProperty(name = "电话")
    private String phone;

    @ApiModelProperty(name = "个人网址")
    private String website;

    @ApiModelProperty(name = "详情")
    private String content;


    public Scholar(String organizationName, String collegeName, String departmentName, String name, String title, String website) {
        this.organizationName = organizationName;
        this.collegeName = collegeName;
        this.departmentName = departmentName;
        this.name = name;
        this.title = title;
        this.website = website;
    }

    public Scholar(String organizationName, String name, String title, String website) {
        this.organizationName = organizationName;
        this.collegeName = "";
        this.name = name;
        this.title = title;
        this.website = website;
    }
}