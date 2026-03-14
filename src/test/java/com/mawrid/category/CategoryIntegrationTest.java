package com.mawrid.category;

import com.mawrid.AbstractIntegrationTest;
import com.mawrid.category.dto.CategoryResponse;
import com.mawrid.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void getCategoryTreeReturnsRootNodes() {
        ResponseEntity<ApiResponse<List<CategoryResponse>>> resp = rest.exchange(
                "/api/v1/categories",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        // DataInitializer seeds 10 root sectors
        assertThat(resp.getBody().getData()).hasSizeGreaterThanOrEqualTo(10);
    }

    @Test
    void getCategoryTreeIsPublic() {
        // No auth header — should still return 200
        ResponseEntity<String> resp = rest.getForEntity("/api/v1/categories", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void createCategoryRequiresAdminRole() {
        // Attempt without auth should return 401/403
        ResponseEntity<Object> resp = rest.postForEntity(
                "/api/v1/categories",
                "{\"name\":\"Test\",\"slug\":\"test\"}",
                Object.class
        );
        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }
}
