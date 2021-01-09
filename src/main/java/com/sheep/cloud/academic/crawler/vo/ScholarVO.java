package com.sheep.cloud.academic.crawler.vo;

import com.sheep.cloud.academic.crawler.entity.Scholar;
import com.sheep.cloud.core.vo.BaseVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author YangChao
 * @create 2019-12-26 14:06:14
 */
@Data
public class ScholarVO extends BaseVO<Scholar> {

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

}
