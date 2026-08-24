package com.zhaoming.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 数据库连通性自检，同时作为 MyBatis-Plus Mapper 用法示例，正式开发时可删除
 */
@Mapper
public interface DbCheckMapper {

    @Select("SELECT 1")
    Integer ping();
}
