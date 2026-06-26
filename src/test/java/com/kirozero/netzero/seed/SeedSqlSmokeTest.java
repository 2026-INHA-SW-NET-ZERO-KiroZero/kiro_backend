package com.kirozero.netzero.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class SeedSqlSmokeTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void demoSeedLoadsWithoutCreditTransactionLedger() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/001_ingredient_master_seed.sql"),
                new ClassPathResource("db/seed/003_demo_slots_seed.sql"),
                new ClassPathResource("db/seed/004_demo_users_seed.sql")
        );
        populator.execute(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        assertThat(countRows(jdbcTemplate, "ingredient_master")).isEqualTo(100);
        assertThat(countRows(jdbcTemplate, "slots")).isEqualTo(12);
        assertThat(countRows(jdbcTemplate, "users")).isEqualTo(8);
        assertThat(countRows(jdbcTemplate, "user_allergies")).isEqualTo(3);
        assertThatThrownBy(() -> countRows(jdbcTemplate, "credit_transactions"))
                .hasMessageContaining("credit_transactions");
    }

    private Integer countRows(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }
}
