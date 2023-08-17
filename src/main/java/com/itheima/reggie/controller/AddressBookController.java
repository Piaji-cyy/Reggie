package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.AddressBookService;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/addressBook")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    @GetMapping("/list")
    public R<List<AddressBook>> list(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("addressBook={}", addressBook);

        //条件构造器
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(addressBook.getUserId() != null, AddressBook::getUserId, addressBook.getUserId());
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);

        List<AddressBook> addressBooks = addressBookService.list(queryWrapper);
        return R.success(addressBooks);
    }

    @PostMapping
    public R<AddressBook> addAddress(@RequestBody AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("addressBook:{}", addressBook);
        addressBookService.save(addressBook);
        return R.success(addressBook);
    }

    @PutMapping("/default")
    public R<AddressBook> setDefaultAddress(@RequestBody AddressBook addressBook) {
        //获取当前用户id
        addressBook.setUserId(BaseContext.getCurrentId());
        //条件构造器
        LambdaUpdateWrapper<AddressBook> queryWrapper = new LambdaUpdateWrapper<>();
        //条件：当前用户的地址
        queryWrapper.eq(addressBook.getUserId() != null, AddressBook::getUserId, addressBook.getUserId());
        //将当前用户地址的is_default字段全部设为0
        queryWrapper.set(AddressBook::getIsDefault, 0);
        //执行更新操作
        addressBookService.update(queryWrapper);
        //随后再将当前地址的is_default字段设为1
        addressBook.setIsDefault(1);
        //再次执行更新操作
        addressBookService.updateById(addressBook);
        return R.success(addressBook);
    }
    @GetMapping("/{id}")
    public R<AddressBook> getById(@PathVariable Long id){
        log.info("根据id查询当前用户地址信息...");
        AddressBook addressBook = addressBookService.getById(id);
        if(addressBook!=null) {
            return R.success(addressBook);
        }else
            return R.error("没有查询到对应用户地址信息");
    }

    @PutMapping
    public R<String> update(@RequestBody AddressBook addressBook){
        log.info("addressBook:{}", addressBook);
        addressBookService.updateById(addressBook);
        return R.success("地址信息修改成功");
    }

    @GetMapping("/default")
    public R<AddressBook> getDefaultAddress() {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        //条件构造器
        LambdaUpdateWrapper<AddressBook> queryWrapper = new LambdaUpdateWrapper<>();
        //条件：当前用户的地址
        queryWrapper.eq(userId != null, AddressBook::getUserId, userId);
        queryWrapper.eq(AddressBook::getIsDefault,1);
        //执行更新操作
        AddressBook addressBook = addressBookService.getOne(queryWrapper);
        return R.success(addressBook);
    }

    @DeleteMapping
    public R<String> delete(@RequestParam Long ids){
        if(ids == null){
            throw new CustomException("地址信息不存在，请刷新重试");
        }
        AddressBook addressBook = addressBookService.getById(ids);
        if (addressBook == null) {
            throw new CustomException("地址信息不存在，请刷新重试");
        }
        addressBookService.removeById(ids);
        return R.success("删除地址成功");
    }
}