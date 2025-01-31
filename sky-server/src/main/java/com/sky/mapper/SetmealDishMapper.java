package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 插入菜品口味数据
     * @param dishIds
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);
}
