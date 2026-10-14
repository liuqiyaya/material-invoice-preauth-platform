package com.liuqiya.invoice.service;

import com.liuqiya.invoice.pojo.dto.AdminRegisterDTO;
import com.liuqiya.invoice.pojo.dto.LoginDTO;

public interface AdminService {

    Long register(AdminRegisterDTO dto);

    String login(LoginDTO loginDTO);


}
