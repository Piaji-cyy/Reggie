package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//菜品管理
@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    //新增菜品
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);

        //方式一、清理所有菜品的缓存数据
        //Set keys = redisTemplate.keys("dish_*");

        //方式二、清理某个分类下菜品的缓存数据
        String key = "dish_" +dishDto.getCategoryId()+ "_1";
        Set keys = redisTemplate.keys(key);

        redisTemplate.delete(keys);
        return R.success("新增菜品成功");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        //1、构造分页构造器
        Page<Dish> pageInfo = new Page<Dish>(page,pageSize);
        //2.对页面进行初步赋值后会发现页面的菜品分类列为空，这是因为Dish对象里面并没有categoryName属性，因此要使用DishDto
        Page<DishDto> dishDtoPage = new Page<>();
        //1、条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //1、添加过滤条件
        queryWrapper.like(name!=null,Dish::getName,name);
        //1、条件排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        dishService.page(pageInfo,queryWrapper);
        //2、对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");
        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item)->{
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);  //对象拷贝
            Long categoryId = item.getCategoryId(); //分类id
            Category category = categoryService.getById(categoryId); //根据id查询分类对象
            if(category!=null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    //根据id查询菜品信息和对应的口味信息
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.updateWithFlavor(dishDto);

        //方式一、清理所有菜品的缓存数据
        //Set keys = redisTemplate.keys("dish_*");

        //方式二、清理某个分类下菜品的缓存数据
        String key = "dish_" +dishDto.getCategoryId()+ "_1";
        Set keys = redisTemplate.keys(key);

        redisTemplate.delete(keys);

        return R.success("新增菜品成功");
    }

    //修改售卖状态
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable Integer status, @RequestParam List<Long> ids){
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(ids != null, Dish::getId, ids);
        updateWrapper.set(Dish::getStatus, status);

        //删除对应的Redis
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId,ids);
        List<Dish> list = dishService.list(queryWrapper);
        for (Dish dish : list) {
            String key = "dish_"+dish.getCategoryId()+"_1";
            redisTemplate.delete(key);
        }
        /*
        这里之后我们就必须要对删除操作也进行缓存清理，因为删除操作执行之前，必须先将菜品状态修改为停售，
        而停售状态这里也会帮我们清理缓存，同时也看不到菜品，随后将菜品删除，仍然看不到菜品，故删除操作不需要进行缓存清理
        */

        dishService.update(updateWrapper);
        return R.success("修改售卖状态成功");
    }

    //删除菜品
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        log.info("删除菜品，id为：{}",ids);
        dishService.remove(ids);
        return R.success("菜品信息删除成功");
    }


//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//        //构造查询条件
//        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper();
//        queryWrapper.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());
//        //添加条件，查询起售状态status=1的菜品
//        queryWrapper.eq(Dish::getStatus,1);
//        //添加排序条件
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//        List<Dish> list = dishService.list(queryWrapper);
//        return R.success(list);
//    }


    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtolist =null;
        //动态构造key
        String key="dish_"+dish.getCategoryId()+"_"+dish.getStatus();
        //先从redis中获取缓存数据
        dishDtolist = (List<DishDto>) redisTemplate.opsForValue().get(key);

        if(dishDtolist != null){
            //如果存在，直接返回，无需查询数据库
            return R.success(dishDtolist);
        }

        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());
        //添加条件，查询起售状态status=1的菜品
        queryWrapper.eq(Dish::getStatus,1);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);
        dishDtolist = list.stream().map((item)->{
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);  //对象拷贝
            Long categoryId = item.getCategoryId(); //分类id
            Category category = categoryService.getById(categoryId); //根据id查询分类对象
            if(category!=null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //当前菜品id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            List<DishFlavor> dishFlavors = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavors);
            return dishDto;
        }).collect(Collectors.toList());

        //如果不存在，需要查询数据库，将查询到的菜品数据缓存到redis
        redisTemplate.opsForValue().set(key,dishDtolist,60, TimeUnit.MINUTES);
        return R.success(dishDtolist);
    }
}
