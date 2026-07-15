package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryCacheIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void 검색어_결과는_캐시하지_않고_전체_목록만_캐시한다() throws Exception {
        Cache cache = cacheManager.getCache("activeCategories");
        assertThat(cache).isNotNull();
        cache.clear();

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/categories").param("keyword", "unique-keyword-1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/categories").param("keyword", "unique-keyword-2"))
                .andExpect(status().isOk());

        assertThat(cache.get("all")).isNotNull();
        assertThat((Map<?, ?>) cache.getNativeCache()).hasSize(1);
    }
}
