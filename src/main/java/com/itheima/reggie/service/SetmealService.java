package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SetmealService extends IService<Setmeal> {
    //新增套餐，保存套餐和菜品的关联
    public void saveWithDish(SetmealDto setmealDto);

    //删除套餐和菜品关联数据
    public void removeWithDish(List<Long> ids);
}
