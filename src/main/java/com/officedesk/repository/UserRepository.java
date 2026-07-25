package com.officedesk.repository;

import com.officedesk.entity.User;
import com.officedesk.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByDepartmentId(Long deptId);

    List<User> findByDepartmentIdAndRoleAndIsActive(Long deptId, Role role, boolean active);

    List<User> findByRole(Role role);

    Optional<User> findByResetToken(String resetToken);
}
