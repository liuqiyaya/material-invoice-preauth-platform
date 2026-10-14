package com.liuqiya.invoice.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class AdminRegisterDTO {

    /**
     * 用户名（唯一）
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 6, max = 20, message = "用户名长度应在6-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "用户名只能包含字母、数字、中文和下划线")
    private String username;

    /**
     * 密码（8个字符以上，不得填写特殊字符）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度应在8-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "密码只能包含字母和数字")
    private String password;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickname;



}
