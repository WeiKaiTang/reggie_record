package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.objenesis.instantiator.basic.NullInstantiator;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;
//import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

// controller 主要是响应客户端页面发过来的请求
@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    //@Autowired 注解是一个用于容器 ( container ) 配置的注解。而在 Spring 的世界当中，自动装配指的就是使用将 Spring 容器中的 bean 自动
    // 的和我们需要这个 bean 的类组装在一起。
    @Autowired
    private EmployeeService employeeService;

    /**
     *
     * @param request
     * @param employee
     * @return
     */
    //request因为前端发过来的请求是post，由login发来的
    //@RequestBody是因为传过来的数据是json格式
    //request对象是为了把employee存储进session
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee){
        /**
         * 1、Md5加密处理
         * 2、从数据库查用户名
         *
         */
        //1,
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        //wrapper.eq("实体类::查询字段", "条件值"); //相当于where条件
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        //3、如果没有查询到则返回登录失败结果
        if (emp == null){
            return R.error("登录失败");
        }

        //4、密码比对，不一致返回失败结果
        if (!emp.getPassword().equals(password)){
            return R.error("登录失败，密码错误");
        }

        //5、查看员工状态，看看是不是已禁用
        if (emp.getStatus() == 0){
            return R.error("登录失败，账号已禁用；");
        }

        //6、登录成功
        request.getSession().setAttribute("employee", emp.getId());
        return R.success(emp);
    }

    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        //1、清理当前session中保存的当前员工的ID
        request.getSession().removeAttribute("employee");
        return R.success("退出成功！");
    }

    /**
     * 新增员工
     * @param employee
     * @return
     */
    //@PostMapping里面不用写，因为添加员工这里请求直接发到employee
    //@RequestBody是因为传进来的参数是json的
    @PostMapping
    public R<String> save (HttpServletRequest request, @RequestBody Employee employee){
        log.info("新增员工，员工信息：{}", employee.toString());
        //1 设置初始密码（需要MD5）及其他表内字段
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        Long empId = (Long) request.getSession().getAttribute("employee");
        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);
        employeeService.save(employee);

        //添加重ID员工会报错，因为有唯一性索引，所以这里要处理异常。两种办法：
        //1、在controller中加入try、catch进行异常捕获，但是有缺点：新增数据种类很多，try.catch会写很多遍。代码如下
        //try {
        //    employeeService.save(employee);
        //}catch (Exception ex){
        //    return R.error("新增员工失败");
        //}
        //第二种办法：全局异常捕获。不管哪个模块，只要出现这种异常，统一在一个地方进行捕获。

        return R.success("新增员工成功");
    }

    /**
     * 员工信息分页查询
     * 这里R<>里面的泛型不是employee，因为对应的请求里包含records、total这些employee里没有的。
     * 请求所在文件：E:\project_file1\Java\reggie_take_out\src\main\resources\backend\page\member\list.html 143行
     * 这里用page，是 MybatisPlus里的，
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        //点页数传过来的请求长这样：是一个get请求  http://127.0.0.1:8080/employee/page?page=1&pageSize=10
        //点搜索传过来的请求长这样：是一个get请求  http://127.0.0.1:8080/employee/page?page=1&pageSize=10&name=123
        // 这两个决定了参数
        log.info(String.valueOf(page) + String.valueOf(pageSize) + String.valueOf(name));

        //构造分页构造器
        Page pageInfo = new Page(page, pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper();
        //添加一个过滤条件，如果有name的话
        //StringUtils.isEmpty();

        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);
        //queryWrapper.like(!name.isEmpty(), Employee::getName, name);

        //添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        //执行查询
        employeeService.page(pageInfo, queryWrapper);
        //其内部会把数据塞进pageInfo
        return R.success(pageInfo);
    }

    /**
     * 根据id修改员工信息
     * E:\project_file1\Java\reggie_take_out\src\main\resources\backend\page\member 130
     */
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee){
        //传进来的employee只有id status两个字段，其他的没传
        log.info(employee.toString());
        Long empID = (Long)request.getSession().getAttribute("employee");
        employee.setUpdateUser(empID);
        employee.setUpdateTime(LocalDateTime.now());
        employeeService.updateById(employee);

        return R.success("J员工信息修改成功。");
    }
}
