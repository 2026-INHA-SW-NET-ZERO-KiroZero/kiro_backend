package com.kirozero.netzero.domain.demo.service;

import com.kirozero.netzero.domain.demo.dto.DemoSeedResponse;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoSeedService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DemoSeedResponse seed() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/001_ingredient_master_seed.sql"),
                new ClassPathResource("db/seed/003_demo_slots_seed.sql")
        );
        populator.execute(dataSource);

        return new DemoSeedResponse(
                countRows("ingredient_master"),
                countRows("slots"),
                0,
                "데모 식재료/슬롯 seed 생성 완료"
        );
    }

    private int countRows(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }
}
