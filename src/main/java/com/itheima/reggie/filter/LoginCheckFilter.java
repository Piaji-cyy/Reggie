package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
* 检查用户是否已经完成登录
* */
@WebFilter(filterName = "LoginCheckFilter",urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    //路径匹配器：专门用来进行路径比较的，支持通配符
    public  static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;

        //1.获取本次请求的url
        String requestURL = request.getRequestURI();


        //定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**", //提问：如何让通配符可以对应到对应的页面？ AntPathMatcher
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login"
        };
        //2、判断本次请求是否需要处理
        boolean check = check(urls, requestURL);
        //3、如果不需要处理则直接放行  当check=true时不用处理
        if (check){
            filterChain.doFilter(request,response);
            return;
        }
        //4-1、判断登录状态，如果已登录
        if(request.getSession().getAttribute("employee") != null){
            Long empId = (Long)request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);
            filterChain.doFilter(request,response);
            return;
        }

        //4-1、判断移动端用户登录状态，如果已登录
        if(request.getSession().getAttribute("user") != null){

            Long userId = (Long)request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request,response);
            return;
        }

        //5、如果未登录则返回未登录结果,通过输出流方式向客户端页面响应数据
        log.info("用户未登录");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
    }

    //进行路径匹配：检查本次请求是否需要放行
    public boolean check(String[] urls, String requestURL){
        for(String url : urls){
            boolean match = PATH_MATCHER.match(url, requestURL);
            if(match){
                return true;
            }
        }
        return false;
    }
}
