package com.tutoring.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tutoring.dto.CreateLessonRequest;
import com.tutoring.entity.Lesson;

public interface LessonService extends IService<Lesson> {

    /**
     * 导师为指定课程创建一个新的 Lesson
     *
     * @param tutorId 当前登录导师的ID
     * @param request 创建Lesson的请求体
     * @return 创建后的 Lesson 实体
     */
    Lesson createLesson(Long tutorId, CreateLessonRequest request);
}