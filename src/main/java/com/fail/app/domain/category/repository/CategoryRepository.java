package com.fail.app.domain.category.repository;

import com.fail.app.domain.category.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    @Query("""
            select category from Category category
            where category.isActive = true
              and (lower(category.name) like lower(concat('%', :keyword, '%'))
                   or lower(category.slug) like lower(concat('%', :keyword, '%')))
            order by category.displayOrder asc
            """)
    List<Category> searchActive(@Param("keyword") String keyword);
}
