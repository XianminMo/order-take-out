package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private  SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private DishMapper dishMapper;


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

        // 新建套餐默认设置为停售状态，起售需要手动操作（在套餐起售停售那个接口判断包含的菜品是否都处于起售状态）
        setmeal.setStatus(0);
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

    /**
     * 修改套餐信息以及菜品-套餐关联信息
     *
     * @param setmealDTO
     */
    @Override
    public void updateWithDish(SetmealDTO setmealDTO) {
        // 修改套餐信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        // 修改菜品-套餐关联信息
        // 1. 删除所有该套餐与菜品的关联信息
        setmealDishMapper.deleteBatchBySetmealId(Collections.singletonList(setmealDTO.getId()));

        // 2. 重新根据setmealDTO的setmealId插入
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        // 设置套餐id
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealDTO.getId());
        });
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐起售、停售
     *
     * @param id
     * @param status
     */
    @Override
    public void startOrStop(Long id, Integer status) {
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

        // 如果要起售套餐且套餐内有未起售菜品，抛出异常
        if (status == 1) {
            for (SetmealDish setmealDish : setmealDishes) {
                if (dishMapper.getById(setmealDish.getDishId()).getStatus() == 0) {
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();

        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
