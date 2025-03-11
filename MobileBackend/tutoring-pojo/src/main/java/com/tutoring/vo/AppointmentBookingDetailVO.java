package com.tutoring.vo;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AppointmentBookingDetailVO {
    private Long appointmentId;
    private String projectName;
    private String description;
    private String appointmentStatus; // Pending, Approved, Rejected, Cancelled, Completed, Expired
    private LocalDateTime bookingCreatedAt; // 预约申请提交的时间
    private LocalDateTime sessionStartTime; // 对应可用时间开始时间
    private LocalDateTime sessionEndTime;   // 对应可用时间结束时间
    private String trainerName;             // 从用户表中查出的教练名称
}
