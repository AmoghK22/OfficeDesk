package com.officedesk.entity;

import com.officedesk.enums.DepartmentName;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private DepartmentName name;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "head_user_id")
    private User head;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @Builder.Default
    private List<User> agents = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
}
