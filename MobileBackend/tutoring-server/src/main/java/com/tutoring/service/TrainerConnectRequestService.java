package com.tutoring.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tutoring.dto.TrainerConnectDecisionDTO;
import com.tutoring.dto.TrainerConnectRequestDTO;
import com.tutoring.entity.TrainerConnectRequest;

public interface TrainerConnectRequestService extends IService<TrainerConnectRequest> {
    /**
     * 统计指定 member 当前待审核（Pending）状态的连接申请数量
     */
    int countPendingRequests(Long memberId);

    /**
     * 提交连接申请，将 member 与 trainer 绑定，状态为 Pending
     */
    void submitConnectRequest(TrainerConnectRequestDTO dto, Long memberId);

    void acceptConnectRequest(TrainerConnectDecisionDTO dto, Long trainerId);

    void rejectConnectRequest(TrainerConnectDecisionDTO dto, Long trainerId);

    /**
     * 判断当前 member 是否已和指定 trainer 建立连接
     *
     * @param memberId 当前 member 的ID
     * @param trainerId 指定 trainer 的ID
     * @return true 表示已连接，false 表示未连接
     */
    boolean isConnected(Long memberId, Long trainerId);
}

