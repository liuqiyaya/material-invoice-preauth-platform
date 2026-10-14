package com.liuqiya.invoice.Interceptor;

import com.liuqiya.invoice.utils.JwtUtil;
import com.liuqiya.invoice.utils.ThreadLocalUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
   @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //放行 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 1. 从请求头拿 token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        // 去掉 "Bearer "
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 2. 校验并解析
        try {
            Claims claims = jwtUtil.parseToken(token);
            if(claims.get("id") == null){
                log.warn("Token 缺少必要字段: id");
                response.setStatus(401);
                return false;
            }
            // 3. 存入 ThreadLocal
            ThreadLocalUtil.set(claims);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)throws Exception{
        ThreadLocalUtil.remove();//请求完成后，清除 ThreadLocal 中的用户信息
    }
}
