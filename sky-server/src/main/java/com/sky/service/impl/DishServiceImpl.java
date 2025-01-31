package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.microsoft.schemas.office.visio.x2012.main.SectionType;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;
    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    // 涉及多张表的数据操作，要保证方法的原子性
    public void saveWithFlavor(DishDTO dishDTO) {
        // 准备菜品数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 插入菜品数据 dish
        dishMapper.insert(dish);

        // 设置主键回显，返回插入菜品后的菜品id，即口味表中的dishId
        Long dishId = dish.getId();

        // 插入菜品对应口味 dish_flavor
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分类查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        long total = page.getTotal();
        List<DishVO> records = page.getResult();
        return new PageResult(total, records);
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        // 判断菜品能否删除 - 是否处于起售状态 dish
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == 1) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 判断菜品能否删除 - 是否与套餐关联 setmeal_dish
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && !setmealIds.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 根据id删除菜品 dish
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            // 删除菜品对应的口味 dish_flavor
//            dishFlavorMapper.deleteByDishId(id);
//        }

        // 根据菜品id批量删除菜品和关联的口味 dish, dish_flavor
        dishMapper.deleteBatchById(ids);
        dishFlavorMapper.deleteBatchById(ids);
    }
}
