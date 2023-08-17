package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrderService;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private ShoppingCartService shoppingCartService;

    //用户下单
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);
        orderService.submit(orders);
        return R.success("订单提交成功");
    }

    //用户界面的历史订单
    @GetMapping("/userPage")
    public R<Page> userPage(int page, int pageSize){
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        //构造分页构造器
        Page<Orders> pageInfo = new Page<Orders>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page,pageSize);
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(userId!=null,Orders::getUserId,userId);
        //条件排序条件
        queryWrapper.orderByDesc(Orders::getOrderTime);
        orderService.page(pageInfo,queryWrapper);

        List<OrdersDto> ordersDtos = pageInfo.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);
            //根据orders的Id找到对应的orederDetail，然后将其复制至OrdersDto
            Long orserId = item.getId();
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId, orserId);
            List<OrderDetail> list = orderDetailService.list(orderDetailLambdaQueryWrapper);
            ordersDto.setOrderDetails(list);
            return ordersDto;
        }).collect(Collectors.toList());
        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");
        ordersDtoPage.setRecords(ordersDtos);
        return R.success(ordersDtoPage);
    }

    /*
    * 此方法备注没有copy过来，地址是选择当前默认地址（如果改了默认地址，那么不是之前的地址，好像也挺合理的）
    * */
    @PostMapping("/again")
    public R<String> again(@RequestBody Orders orders){
        //获取订单id
        Long ordersId = orders.getId();
        //条件构造器
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        //查询订单细节数据
        queryWrapper.eq(OrderDetail::getOrderId,ordersId);
        List<OrderDetail> list = orderDetailService.list(queryWrapper);
        //将获取到的订单数据加载到购物车中
        List<ShoppingCart> shoppingCartList = list.stream().map((item) -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            //将获取到的订单数据加载到购物车中
            BeanUtils.copyProperties(item, shoppingCart);
            //设置用户Id，创建购物车时间
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());
        shoppingCartService.saveBatch(shoppingCartList);
        return R.success("好吃吗？好吃就再来一单呀！");
    }

    //后端界面的历史订单
    @GetMapping("/page")
    public R<Page> Page(int page, int pageSize, Long number, String beginTime, String endTime){
        //构造分页构造器
        Page<Orders> pageInfo = new Page<Orders>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page,pageSize);
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.eq(number!=null, Orders::getId, number);
        queryWrapper.ge(StringUtils.isNotEmpty(beginTime), Orders::getOrderTime, beginTime).lt(StringUtils.isNotEmpty(endTime), Orders::getOrderTime, endTime);
        //条件排序条件
        queryWrapper.orderByDesc(Orders::getOrderTime);
        orderService.page(pageInfo,queryWrapper);

        List<OrdersDto> ordersDtos = pageInfo.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);
            //根据orders的Id找到对应的orederDetail，然后将其复制至OrdersDto
            Long orserId = item.getId();
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId, orserId);
            List<OrderDetail> list = orderDetailService.list(orderDetailLambdaQueryWrapper);
            ordersDto.setOrderDetails(list);
            return ordersDto;
        }).collect(Collectors.toList());
        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");
        ordersDtoPage.setRecords(ordersDtos);
        return R.success(ordersDtoPage);
    }

    @PutMapping
    public R<String> changeStatus(@RequestBody Map<String, String> map){
        int status = Integer.parseInt(map.get("status"));
        Long orderId = Long.valueOf(map.get("id"));
        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Orders::getId,orderId);
        updateWrapper.set(Orders::getStatus,status);
        orderService.update(updateWrapper);
        return R.success("订单状态修改成功！");
    }
}