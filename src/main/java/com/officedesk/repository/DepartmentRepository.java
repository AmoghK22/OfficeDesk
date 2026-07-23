package com.officedesk.repository;

import com.officedesk.entity.Department;
import com.officedesk.enums.DepartmentName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(DepartmentName name);
}
