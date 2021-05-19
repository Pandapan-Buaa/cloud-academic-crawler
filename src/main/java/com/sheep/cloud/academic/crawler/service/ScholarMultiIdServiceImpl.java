package com.sheep.cloud.academic.crawler.service;

import com.sheep.cloud.academic.crawler.entity.ScholarMultiId;
import com.sheep.cloud.academic.crawler.vo.ScholarMultiIdVO;
import com.sheep.cloud.web.service.simple.AbstractCrudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScholarMultiIdServiceImpl  extends AbstractCrudService<ScholarMultiId, ScholarMultiIdVO> implements ScholarMultiIdService{
}
