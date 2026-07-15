package com.fail.app.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class SoftDeleteEntity extends BaseTimeEntity {

    @Column
    private LocalDateTime deletedAt;

    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
