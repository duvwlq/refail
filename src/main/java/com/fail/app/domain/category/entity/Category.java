package com.fail.app.domain.category.entity;

import com.fail.app.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private boolean isActive;

    @Builder
    private Category(String name, String slug, Integer displayOrder, boolean isActive) {
        this.name = name;
        this.slug = slug;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
    }

    public void update(String name, String slug, Integer displayOrder) {
        this.name = name;
        this.slug = slug;
        this.displayOrder = displayOrder;
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
