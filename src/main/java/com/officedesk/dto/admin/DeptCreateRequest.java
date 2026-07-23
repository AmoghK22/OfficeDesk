package com.officedesk.dto.admin;

import com.officedesk.enums.DepartmentName;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeptCreateRequest {

    @NotNull
    private DepartmentName name;

    private Long headUserId;
}
