CREATE TABLE IF NOT EXISTS `university` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `name` varchar(255) NOT NULL COMMENT '大学名称',
  `website` varchar(255) DEFAULT '' COMMENT '官方网址',
  `type` varchar(255) DEFAULT '' COMMENT '类型985 211',
  PRIMARY KEY (`id`),
  KEY `university_index_name` (`name`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '大学表';

CREATE TABLE IF NOT EXISTS `department` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `parent_id` varchar(32) DEFAULT NULL COMMENT '父类id',
  `node_code` varchar(255) DEFAULT NULL COMMENT '节点id 形如1000-1000-1000',
  `level_code` int(11) DEFAULT NULL COMMENT '等级 1，2，3',
  `order_index` bigint(20) DEFAULT '0' COMMENT '排序',
  `university_name` varchar(255) NOT NULL COMMENT '大学名称',
  `name` varchar(255) NOT NULL COMMENT '部门名称',
  PRIMARY KEY (`id`),
  KEY `department_index_parent_id` (`parent_id`),
  KEY `department_index_university_name` (`university_name`),
  KEY `department_index_name` (`name`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '部门表';

CREATE TABLE IF NOT EXISTS `scholar_configure` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `organization_name` varchar(255) NOT NULL COMMENT '大学名称',
  `college_name` varchar(255) DEFAULT '' COMMENT '院名称',
  `department_name` varchar(255) DEFAULT '' COMMENT '系名称',
  `title` varchar(255) DEFAULT '' COMMENT '职称 讲师、副教授、教授',
  `website` varchar(255) DEFAULT '' COMMENT '网址',
  `xpath` varchar(255) DEFAULT '' COMMENT '节点',
  PRIMARY KEY (`id`),
  KEY `scholar_configure_index_organization_name` (`organization_name`),
  KEY `scholar_configure_index_college_name` (`college_name`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '学者配置表';

