package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;
    @Transactional
    //新增菜品，同时保存菜品对应的口味数据
    public void saveWithFlavor(DishDto dishDto){
        //保存菜品基本信息到菜品表dish
        this.save(dishDto);
        Long dishId = dishDto.getId(); //菜品id
        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors(); //此时flavors只有name和value，没有dishId
        flavors = flavors.stream().map((item)->{
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());
        //保存菜品口味数据到口味表dish_flavor
        dishFlavorService.saveBatch(flavors);
    }

    //根据id查询菜品信息和对应的口味信息
    public DishDto getByIdWithFlavor(Long id){
        //查询菜品基本信息
        Dish dish = this.getById(id);
        DishDto dishDto = new DishDto();
        //对象拷贝
        BeanUtils.copyProperties(dish,dishDto);
        //查询当前菜品对应的口味信息，从dish_flavor表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);
        return dishDto;
    }

    //更新菜品信息，同时更新对应的口味信息
    @Override
    public void updateWithFlavor(DishDto dishDto){
        //更新dish表基本信息
        this.updateById(dishDto);
        //更新清理当前菜品对应的口味数据————dish_flavor表的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());
        dishFlavorService.remove(queryWrapper);
        //添加当前提交的口味数据————dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors(); //此时flavors只有name和value，没有dishId
        flavors = flavors.stream().map((item)->{
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());
        dishFlavorService.saveBatch(flavors);
    }

    @Override
    public void remove(List<Long> ids) {
        //查询套餐状态，确定是否可以删除
        LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
        dishQueryWrapper.in(Dish::getId,ids);
        dishQueryWrapper.eq(Dish::getStatus,1);
        int count = this.count(dishQueryWrapper);
        //如果不能删除，抛出一个业务异常
        if(count>0){
            throw new CustomException("套餐正在售卖中，不能删除");
        }
        this.removeByIds(ids);

        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DishFlavor::getDishId,ids);
        dishFlavorService.remove(queryWrapper);

    }
}

