package com.tutoring.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tutoring.dao.TrainerConnectRequestDao;
import com.tutoring.dto.TrainerConnectDecisionDTO;
import com.tutoring.dto.TrainerConnectRequestDTO;
import com.tutoring.entity.Notification;
import com.tutoring.entity.TrainerConnectRequest;
import com.tutoring.enumeration.ErrorCode;
import com.tutoring.exception.CustomException;
import com.tutoring.service.NotificationService;
import com.tutoring.service.TrainerConnectRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TrainerConnectRequestServiceImpl extends ServiceImpl<TrainerConnectRequestDao, TrainerConnectRequest>
        implements TrainerConnectRequestService {

    @Autowired
    private NotificationService notificationService;

    // 判断当前 member 是否已和指定 trainer 建立连接
    @Override
    public boolean isConnected(Long memberId, Long trainerId) {
        LambdaQueryWrapper<TrainerConnectRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrainerConnectRequest::getMemberId, memberId)
                .eq(TrainerConnectRequest::getTrainerId, trainerId)
                .eq(TrainerConnectRequest::getStatus, TrainerConnectRequest.RequestStatus.Accepted);
        return this.count(queryWrapper) > 0;
    }

    // 统计指定 member 当前待审核（Pending）状态的连接申请数量
    @Override
    public int countPendingRequests(Long memberId) {
        LambdaQueryWrapper<TrainerConnectRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TrainerConnectRequest::getMemberId, memberId)
                .eq(TrainerConnectRequest::getStatus, TrainerConnectRequest.RequestStatus.Pending);
        return this.count(queryWrapper);
    }

    // member提交连接申请
    @Override
    public void submitConnectRequest(TrainerConnectRequestDTO dto, Long memberId) {
        TrainerConnectRequest request = TrainerConnectRequest.builder()
                .memberId(memberId)
                .trainerId(dto.getTrainerId())
                .status(TrainerConnectRequest.RequestStatus.Pending)
                .requestMessage(dto.getRequestMessage())
                .build();
        this.save(request);
        // 生成并发送通知给教练
        Notification notification = Notification.builder()
                .userId(dto.getTrainerId())
                .title("New connection request")
                // 你有一个新的连接申请
                .message("You have a new connection request.")
                .type(Notification.NotificationType.INFO)
                .isRead(false)
                .build();
        notificationService.sendNotification(notification);
    }


    // Trainer 接受连接申请
    @Override
    public void acceptConnectRequest(TrainerConnectDecisionDTO dto, Long trainerId) {
        TrainerConnectRequest request = this.getById(dto.getRequestId());
        if (request == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Connect request not found.");
        }
        // 确保该申请属于当前教练
        if (!request.getTrainerId().equals(trainerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not authorized to process this request.");
        }
        // 申请必须处于待审核状态
        if (request.getStatus() != TrainerConnectRequest.RequestStatus.Pending) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "This connect request has already been processed.");
        }
        // 更新状态和反馈信息
        request.setStatus(TrainerConnectRequest.RequestStatus.Accepted);
        request.setResponseMessage(dto.getResponseMessage());
        boolean updateResult = this.updateById(request);
        if (!updateResult) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Unable to accept connect request.");
        }
        log.info("Trainer [{}] accepted connect request [{}]", trainerId, dto.getRequestId());

        // 生成并发送通知给申请的 member
        Notification notification = Notification.builder()
                .userId(request.getMemberId())
                .title("Application result notification")
                // 你的其中一个教练连接申请已被接受
                .message("One of your trainer connection requests has been accepted.")
                .type(Notification.NotificationType.INFO)
                .isRead(false)
                .build();
        notificationService.sendNotification(notification);
    }

    // Trainer 拒绝连接申请
    @Override
    public void rejectConnectRequest(TrainerConnectDecisionDTO dto, Long trainerId) {
        TrainerConnectRequest request = this.getById(dto.getRequestId());
        if (request == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "Connect request not found.");
        }
        // 确保该申请属于当前教练
        if (!request.getTrainerId().equals(trainerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "You are not authorized to process this request.");
        }
        // 申请必须处于待审核状态
        if (request.getStatus() != TrainerConnectRequest.RequestStatus.Pending) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "This connect request has already been processed.");
        }
        // 更新状态和反馈信息
        request.setStatus(TrainerConnectRequest.RequestStatus.Rejected);
        request.setResponseMessage(dto.getResponseMessage());
        boolean updateResult = this.updateById(request);
        if (!updateResult) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "Unable to reject connect request.");
        }
        log.info("Trainer [{}] rejected connect request [{}]", trainerId, dto.getRequestId());

        // 生成并发送通知给申请的 member
        Notification notification = Notification.builder()
                .userId(request.getMemberId())
                .title("Application result notification")
                .message("One of your trainer connection requests has been rejected.")
                .type(Notification.NotificationType.INFO)
                .isRead(false)
                .build();
        notificationService.sendNotification(notification);
    }
}

