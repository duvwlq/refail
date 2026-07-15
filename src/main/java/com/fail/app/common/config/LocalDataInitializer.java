package com.fail.app.common.config;

import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer {

    private final CategoryRepository categoryRepository;

    @Bean
    public CommandLineRunner initLocalData() {
        return args -> {
            if (categoryRepository.count() > 0) {
                return;
            }

            categoryRepository.save(Category.builder()
                    .name("공부")
                    .slug("study")
                    .displayOrder(1)
                    .isActive(true)
                    .build());
            categoryRepository.save(Category.builder()
                    .name("다이어트")
                    .slug("diet")
                    .displayOrder(2)
                    .isActive(true)
                    .build());
            categoryRepository.save(Category.builder()
                    .name("일상")
                    .slug("daily")
                    .displayOrder(3)
                    .isActive(true)
                    .build());
        };
    }
}
