package org.soursoup.bimbim.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table
public class Question extends AbstractEntity {
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String answerLeft;

    @Column(nullable = false)
    private String answerRight;

    @Column
    private String image;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
