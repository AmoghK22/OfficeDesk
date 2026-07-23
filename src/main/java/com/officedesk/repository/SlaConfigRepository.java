package com.officedesk.repository;

import com.officedesk.entity.SlaConfig;
import com.officedesk.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SlaConfigRepository extends JpaRepository<SlaConfig, Long> {

    Optional<SlaConfig> findByDepartmentIdAndPriority(Long departmentId, Priority priority);

    List<SlaConfig> findByDepartmentId(Long departmentId);
}
