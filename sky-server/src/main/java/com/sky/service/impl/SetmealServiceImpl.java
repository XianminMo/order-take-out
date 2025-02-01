package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private  SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;


    /**
     * 新增套餐
     *
     * @param setmealDTO
     */
    @Override
    public void save(SetmealDTO setmealDTO) {
        // 添加套餐至 setmeal table
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 设置为起售状态
        setmeal.setStatus(1);
        setmealMapper.insert(setmeal);

        Long setmealId = setmeal.getId();

        // 添加菜品-套餐关联 -> setmeal_dish
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        // 设置套餐id
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        setmealDishMapper.insertBatch(setmealDishes);

    }

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<Setmeal> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        long total = page.getTotal();
        List<Setmeal> records = page.getResult();
        return new PageResult(total, records);
    }

    /**
     * 批量删除套餐
     *
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        // 判断套餐能否删除 - 起售状态不可删除 -> setmeal table
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == 1) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        // 删除套餐 -> setmeal table
        setmealMapper.deleteBatchById(ids);

        // 删除菜品套餐关联 -> setmeal_dish table
        setmealDishMapper.deleteBatchBySetmealId(ids);
    }

    /**
     * 根据id查询套餐
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Long id) {
        SetmealVO setmealVO = new SetmealVO();
        
        // 查询套餐基本信息
        Setmeal setmeal = setmealMapper.getById(id);
        BeanUtils.copyProperties(setmeal, setmealVO);

        // 查询菜品-套餐关联信息
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

        // 组装
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }
}
