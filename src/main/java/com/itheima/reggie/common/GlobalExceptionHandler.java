package com.itheima.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理，AOP思想
 */
//拦截哪些controller？拦截那些类上加了RestController注解的controller
@ControllerAdvice(annotations = {RestController.class, Controller.class})
//一会要写个方法，该方法要返回json数据
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 异常处理方法
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error(ex.getMessage());
        if (ex.getMessage().contains("Duplicate entry")){
            String split[] = ex.getMessage().split(" ");
            String mess = split[2] + "已存在";
            return R.error(mess);
        }

        return R.error("未知错误");
    }
}
