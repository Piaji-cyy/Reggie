package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/shoppingCart")


public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    //添加购物车
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车数据：{}",shoppingCart);
        //获取用户id，指定当前是哪个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();
        //将用户id存进shoppingCart对象里
        shoppingCart.setUserId(currentId);
        //获取当前菜品id
        Long dishId = shoppingCart.getDishId();
        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,currentId);
        //判断当前添加的是菜品还是套餐
        if(dishId != null){
            //添加到购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
        }else{
            //添加到购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }
        //查询当前菜品或套餐是否在购物车内
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);
        if(cartServiceOne != null){
            //如果已存在，就在原来数量上加1
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number + 1);
            shoppingCartService.updateById(cartServiceOne);
        }else{
            //如果不存在，则添加到购物车，数量默认是1
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setNumber(1);
            shoppingCartService.save(shoppingCart);
            //这里是为了统一结果，最后都返回cartServiceOne会比较方便
            cartServiceOne = shoppingCart;
        }
        return R.success(cartServiceOne);
    }

    //查看购物车
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        Long currentId = BaseContext.getCurrentId();
        queryWrapper.eq(ShoppingCart::getUserId,currentId);
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);
        return R.success(list);
    }

    //清空购物车
    @DeleteMapping("/clean")
    public R<String> clean(){
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        shoppingCartService.remove(queryWrapper);
        return R.success("清空购物车成功");
    }

    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart){
        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();
        //条件构造器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //查询当前用户的购物车
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        if(dishId != null){
            //当前数量减少的是菜品
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
            ShoppingCart dishCart = shoppingCartService.getOne(queryWrapper);
            dishCart.setNumber(dishCart.getNumber()-1);
            Integer number = dishCart.getNumber();
            if(number>0){
                shoppingCartService.updateById(dishCart);
            }else {
                shoppingCartService.removeById(dishCart.getId());
            }
            return R.success(dishCart);
        }
        if(setmealId != null){
            //当前数量减少的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId,setmealId);
            ShoppingCart setmealCart = shoppingCartService.getOne(queryWrapper);
            setmealCart.setNumber(setmealCart.getNumber()-1);
            Integer number = setmealCart.getNumber();
            if(number>0){
                shoppingCartService.updateById(setmealCart);
            }else {
                shoppingCartService.removeById(setmealCart.getId());
            }
            return R.success(setmealCart);
        }
        return R.error("系统繁忙，请稍后再试");
    }
}
