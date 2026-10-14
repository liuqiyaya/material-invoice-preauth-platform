package com.liuqiya.invoice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuqiya.invoice.exception.BizException;
import com.liuqiya.invoice.mapper.AdminMapper;
import com.liuqiya.invoice.pojo.dto.AdminRegisterDTO;
import com.liuqiya.invoice.pojo.dto.LoginDTO;
import com.liuqiya.invoice.pojo.entity.Admin;
import com.liuqiya.invoice.service.AdminService;
import com.liuqiya.invoice.utils.JwtUtil;
import com.liuqiya.invoice.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(AdminRegisterDTO dto) {
        // 1. 校验重名
        Long count = adminMapper.selectCount(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, dto.getUsername())
        );
        if (count != null && count > 0) {
            throw new BizException(400, "用户名已存在");
        }

        // 2. 校验密码是否一致
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BizException(400, "两次密码输入不一致");
        }

        // 3. 加密密码
        String hash = PasswordUtil.encode(dto.getPassword());

        // 4. 保存
        Admin admin = new Admin();
        admin.setUsername(dto.getUsername());
        admin.setPassword(hash);
        admin.setNickname(dto.getNickname() == null ? dto.getUsername() : dto.getNickname());
        admin.setStatus(1);
        adminMapper.insert(admin);

        log.info("【管理员注册】username={}, id={}", dto.getUsername(), admin.getId());
        return admin.getId();
    }

    /**
     * 管理员登录
     */
    @Override
    public String login(LoginDTO loginDTO) {
        log.info("进入service层,管理员登录请求: {}", loginDTO.getUsername());
        //校验管理员是否存在或启用
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, loginDTO.getUsername())
                .eq(Admin::getStatus, 1)
                .eq(Admin::getDeleted, 0));
        if (admin == null) {
            throw new BizException("用户名不存在或已被禁用");
        }
        //校验密码是否正确
        if(!PasswordUtil.matches(loginDTO.getPassword(),admin.getPassword())){
            throw new BizException("密码错误");
        }
        //生成token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", admin.getId());
        claims.put("username", admin.getUsername());
        String token = jwtUtil.genToken(claims);
        log.info("生成的token: {}", token);
        return token;
    }

}
