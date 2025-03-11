package com.tutoring.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.tutoring.dto.SignupRequest;
import com.tutoring.dto.UserProfileDTO;
import com.tutoring.dto.VerifyCodeRequest;
import com.tutoring.entity.Specializations;
import com.tutoring.entity.User;

import java.util.List;

public interface UserService extends IService<User> {
    // 注册相关
    void sendSignupVerification(SignupRequest signupRequest);
    void verifySignupCode(VerifyCodeRequest verifyReq);

    // 用户资料更新
    void updateUserProfile(Long userId, UserProfileDTO userProfileDTO);

    // 原有方法
    User createUser(User user);
    User getUserById(Long userID);
    User getByEmail(String email);

    /**
     * 查询所有专长常量
     * @return List of Specializations
     */
    List<Specializations> listSpecializations();
}

