package com.tutoring.controller;

import com.tutoring.dto.RegistrationApprovalRequest;
import com.tutoring.entity.CourseRegistration;
import com.tutoring.entity.User;
import com.tutoring.enumeration.ErrorCode;
import com.tutoring.exception.CustomException;
import com.tutoring.result.RestResult;
import com.tutoring.service.CourseRegistrationService;
import com.tutoring.util.SecurityUtils;
import com.tutoring.vo.RegistrationResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/course/registrations")
@Slf4j
@Validated
public class CourseRegistrationController {

    @Autowired
    private CourseRegistrationService courseRegistrationService;

    /**
     * GET /course/registrations
     * 导师查看所有注册请求
     */
    @GetMapping
    public RestResult<List<RegistrationResponseDTO>> listAllRegistrations() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated.");
        }
        if (SecurityUtils.getCurrentUserRole() != User.Role.tutor) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Only tutors can view registration requests.");
        }

        // 调用 Service 获取包含申请者昵称的信息
        List<RegistrationResponseDTO> responseDTOList = courseRegistrationService.findRegistrationsByTutorWithUserInfo(currentUserId);
        return RestResult.success(responseDTOList, "Registrations retrieved successfully.");
    }

    /**
     * PUT /course/registrations/{registrationId}
     * 导师审批注册请求
     */
    @PutMapping("/{registrationId}")
    public RestResult<?> updateRegistration(@PathVariable Long registrationId,
                                            @Valid @RequestBody RegistrationApprovalRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated.");
        }
        if (SecurityUtils.getCurrentUserRole() != User.Role.tutor) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Only tutors can update registration requests.");
        }
        courseRegistrationService.updateRegistrationStatus(registrationId, request.getDecision(), currentUserId);
        return RestResult.success(null, "Registration request updated successfully.");
    }

    /**
     * GET /course/registrations/student
     * 学生查看自己所有的订单
     */
    @GetMapping("/student")
    public RestResult<List<CourseRegistration>> listStudentRegistrations() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "User is not authenticated.");
        }
        if (SecurityUtils.getCurrentUserRole() != User.Role.student) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Only students can view their orders.");
        }
        List<CourseRegistration> registrations = courseRegistrationService.findRegistrationsByStudent(currentUserId);
        return RestResult.success(registrations, "Student registrations retrieved successfully.");
    }
}

