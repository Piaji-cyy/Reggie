package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;
    //新增套餐，保存套餐和菜品的关联
    @Transactional
    public void saveWithDish(SetmealDto setmealDto){
        //保存套餐基本信息，操作Setmeal表，执行insert操作
        this.save(setmealDto);
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();  //存贮的值实际上只有dishId，没有setmealId
        setmealDishes.stream().map((item)->{
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        //保存套餐和菜品关联的信息，操作setmeal_dish，执行insert操作
        setmealDishService.saveBatch(setmealDishes);
    }


    //删除套餐和菜品关联数据，只有停售的套餐才能删除
    @Transactional
    public void removeWithDish(List<Long> ids){
        //查询套餐状态，确定是否可以删除
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId,ids);
        queryWrapper.eq(Setmeal::getStatus,1);
        int count = this.count(queryWrapper);

        //如果不能删除，抛出一个业务异常
        if(count>0){  //status为1，意味着正在售卖
            throw new CustomException("套餐正在售卖中，不能删除");
        }

        //如果可以删除，先删除套餐表中的数据 --setmeal
        this.removeByIds(ids);

        LambdaQueryWrapper<SetmealDish> setDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setDishLambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        //删除关系表中的数据--Setmeal_dish
        setmealDishService.remove(setDishLambdaQueryWrapper);
    }
}
