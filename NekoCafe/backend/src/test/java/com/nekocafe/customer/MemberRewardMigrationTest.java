package com.nekocafe.customer;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MemberRewardMigrationTest {

    @Test
    void includesRepairMigrationForMemberRewardSchema() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V019__repair_member_reward_redemption_schema.sql");

        assertThat(migration).exists();
        String sql = Files.readString(migration);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS reward_catalog");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS reward_redemption");
        assertThat(sql).contains("INFORMATION_SCHEMA.COLUMNS");
        assertThat(sql).contains("reward_redemption_id");
        assertThat(sql).contains("idx_points_reward_redemption");
        assertThat(sql).contains("8 元甜品券");
    }
}
