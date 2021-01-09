package com.sheep.cloud.academic.crawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * @author YangChao
 * @create 2018-05-29 15:41
 **/
@ConfigurationProperties(prefix = "cloud.crawler")
@Data
@Component
public class FileUploadProperties {

	/**
	 * 普通文件上传地址
	 */
	private String fileStoragePath;

}


