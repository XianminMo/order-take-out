package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id获取套餐id
     * @param dishIds
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 批量插入菜品-套餐关联
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id批量删除
     * @param setmealIds
     */
    void deleteBatchBySetmealId(List<Long> setmealIds);

    /**
     * 根据套餐id查询菜品-套餐关联信息
     * @param id
     * @return
     */
    List<SetmealDish> getBySetmealId(Long setmealId);
}
