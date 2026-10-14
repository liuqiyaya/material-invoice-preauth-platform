package com.liuqiya.invoice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuqiya.invoice.result.Result;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@Order(1)
public class PathSecurityFilter implements Filter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();

        if (uri.startsWith("/file/")) {
            String filePath = httpRequest.getParameter("filePath");

            if (filePath != null && !filePath.isBlank()) {
                if (!isPathSafe(filePath)) {
                    log.warn("检测到路径遍历攻击: uri={}, filePath={}", uri, filePath);

                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                            objectMapper.writeValueAsString(Result.error(400, "非法的文件路径"))
                    );
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isPathSafe(String filePath) {
        try {
            Path normalized = Paths.get(filePath).normalize();
            return !normalized.toString().contains("..");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("路径安全过滤器初始化完成");
    }

    @Override
    public void destroy() {
        log.info("路径安全过滤器销毁");
    }

}

