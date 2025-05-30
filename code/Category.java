package org.soursoup.bimbim.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table
public class Category extends AbstractEntity {
    @Column(nullable = false)
    private String name;

    @Column
    private String avatar;

    @Column
    private Long questionCount = 0L;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions;
}
