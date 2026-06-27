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
                new ClassPathResource("db/seed/003_demo_slots_seed.sql"),
                new ClassPathResource("db/seed/004_demo_users_seed.sql"),
                new ClassPathResource("db/seed/005_demo_flow_seed.sql")
        );
        populator.execute(dataSource);

        return new DemoSeedResponse(
                countRows("ingredient_master"),
                countRows("slots"),
                countDemoUsers(),
                "데모 식재료/슬롯/사용자/세션 seed 생성 완료"
        );
    }

    private int countRows(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private int countDemoUsers() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM users
                WHERE email IN (
                    'demo1@inha.edu',
                    'demo2@inha.edu',
                    'demo3@inha.edu',
                    'demo4@inha.edu',
                    'demo5@inha.ac.kr',
                    'demo6@inha.ac.kr',
                    'demo7@inha.ac.kr',
                    'demo8@inha.ac.kr'
                )
                """, Integer.class);
        return count == null ? 0 : count;
    }
}
