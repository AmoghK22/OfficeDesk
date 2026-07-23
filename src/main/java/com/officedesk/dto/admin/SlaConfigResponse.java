package com.officedesk.dto.admin;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaConfigResponse {

    private Long id;
    private String departmentName;
    private String priority;
    private Integer resolutionHours;
}
