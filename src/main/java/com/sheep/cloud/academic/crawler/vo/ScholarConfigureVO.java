package com.sheep.cloud.academic.crawler.vo;

import com.sheep.cloud.academic.crawler.entity.ScholarConfigure;
import com.sheep.cloud.core.vo.BaseVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author YangChao
 * @create 2019-12-26 14:24:15
 */
@Data
public class ScholarConfigureVO extends BaseVO<ScholarConfigure> {

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

}
