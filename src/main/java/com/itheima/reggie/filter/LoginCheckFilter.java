package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经完成登录的过滤器，防止跳过登录直接访问。
 */
@Slf4j
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        //log.info("拦截到请求:{}", request.getRequestURI());

        //1、获取本次请求的url
        String requestURI = request.getRequestURI();
        log.info("拦截到请求:{}", requestURI);

        //2、判断本次请求是否需要处理（比如访问登录页面不需要登录）
        //3、如果不需要处理则直接放行
        //定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                //下面这俩都是静态资源，可以放行，数据才不能放行
                "/backend/**",
                "/front/**",
        };
        boolean check = check(urls, requestURI);
        if (check){
            log.info("本次请求不需要处理:{}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        //4、判断登录状态，若以登录，则直接放行
        if (request.getSession().getAttribute("employee") != null){
            log.info("用户已登录，用户id为:{}", request.getSession().getAttribute("employee"));
            filterChain.doFilter(request, response);
            return;
        }

        log.info("用户未登录");
        //5、如果未登录则返回未登录结果。通过输出流的方式向客户端响应数据
        //"NOTLOGIN"这里涉及到request.js里定义的前端的拦截器
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
        //下面这条是放行语句。
        //filterChain.doFilter(request, response);
    }

    public boolean check(String[] urls, String requestURI){
        for (String url : urls){
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match){
                return true;
            }
        }
        return false;
    }
}
