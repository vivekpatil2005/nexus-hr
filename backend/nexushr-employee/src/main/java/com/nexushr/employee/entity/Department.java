package com.nexushr.employee.entity;

import com.nexushr.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Department entity with self-referential hierarchy support.
 * Represents organizational units in the company structure.
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Department> children = new ArrayList<>();

    @Column(name = "head_id")
    private Long headId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
