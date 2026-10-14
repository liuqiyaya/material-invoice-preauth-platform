package com.liuqiya.invoice.controller;

import com.liuqiya.invoice.pojo.dto.AdminRegisterDTO;
import com.liuqiya.invoice.result.Result;
import com.liuqiya.invoice.pojo.dto.LoginDTO;
import com.liuqiya.invoice.service.AdminService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     *管理员注册
     */
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody AdminRegisterDTO dto) {
        Long id = adminService.register(dto);
        return Result.success("注册成功", id);
    }

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("管理员登录请求: {}", loginDTO.getUsername());
        String token = adminService.login(loginDTO);
        return Result.success(token);
    }
}
