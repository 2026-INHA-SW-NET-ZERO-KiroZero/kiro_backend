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
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        clearTables(jdbcTemplate);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/001_ingredient_master_seed.sql"),
                new ClassPathResource("db/seed/003_demo_slots_seed.sql"),
                new ClassPathResource("db/seed/004_demo_users_seed.sql"),
                new ClassPathResource("db/seed/005_demo_flow_seed.sql")
        );
        populator.execute(dataSource);

        assertThat(countRows(jdbcTemplate, "ingredient_master")).isEqualTo(100);
        assertThat(countRows(jdbcTemplate, "slots")).isEqualTo(12);
        assertThat(countRows(jdbcTemplate, "users")).isEqualTo(8);
        assertThat(countRows(jdbcTemplate, "user_allergies")).isEqualTo(3);
        assertThat(countRows(jdbcTemplate, "session_participants")).isEqualTo(11);
        assertThat(countRows(jdbcTemplate, "session_ingredients")).isEqualTo(19);
        assertThat(countRows(jdbcTemplate, "menu_votes")).isEqualTo(3);
        assertThat(countRows(jdbcTemplate, "consumption_records")).isEqualTo(1);
        assertThat(countRows(jdbcTemplate, "consumption_record_items")).isEqualTo(5);
        assertThatThrownBy(() -> countRows(jdbcTemplate, "credit_transactions"))
                .hasMessageContaining("credit_transactions");
    }

    private void clearTables(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM consumption_record_items");
        jdbcTemplate.update("DELETE FROM consumption_records");
        jdbcTemplate.update("DELETE FROM menu_votes");
        jdbcTemplate.update("DELETE FROM session_ingredients");
        jdbcTemplate.update("DELETE FROM session_participants");
        jdbcTemplate.update("DELETE FROM user_allergies");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM slots");
        jdbcTemplate.update("DELETE FROM ingredient_master");
    }

    private Integer countRows(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }
}
