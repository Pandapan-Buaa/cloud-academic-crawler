package com.sheep.cloud.academic.crawler.entity;

import com.sheep.cloud.core.entity.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "scholar_multiid")
@Data
@NoArgsConstructor
public class ScholarMultiId  extends BaseEntity {
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

    @ApiModelProperty(name = "个人网址")
    private String website;

    @ApiModelProperty(name = "详情")
    private String content;

    @ApiModelProperty(name = "json")
    private String json;

    @ApiModelProperty(name = "ids")
    private String ids;

    public ScholarMultiId(ScholarTemp scholar,String json,String ids) {
        this.id = scholar.getId();
        this.organizationName = scholar.getOrganizationName();
        this.collegeName = scholar.getCollegeName();
        this.departmentName = scholar.getDepartmentName();
        this.content = scholar.getContent();
        this.website = scholar.getWebsite();
        this.name = scholar.getName();
        this.json = json;
        this.ids = ids;
    }
}
