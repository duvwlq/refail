package com.fail.app.domain.moderation.repository;

import com.fail.app.domain.moderation.entity.ModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, Long> {
}
