/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.siglus.siglusapi.localmachine.cdc;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import io.debezium.config.Configuration;
import io.debezium.connector.postgresql.connection.PostgresConnection;
import io.debezium.jdbc.JdbcConnection;
import io.debezium.relational.TableId;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class PublicationPreparerTest {
  @Spy private PublicationPreparer publicationPreparer;
  @Mock private PostgresConnection conn;
  private List<String> capturedSql;
  private Configuration config;

  @Before
  public void setup() throws SQLException {
    capturedSql = new LinkedList<>();
    configureConn();
    configureConfig();
  }

  @Test
  public void shouldCreatePublicationWhenPrepareGivenPublicationNotExists() throws SQLException {
    // given
    doReturn(conn).when(publicationPreparer).getConn(any());
    HashSet<TableId> existingPublicationTables = new HashSet<>();
    given(conn.queryAndMap(any(), any())).willReturn(existingPublicationTables);
    // when
    publicationPreparer.prepare(config);
    // then
    assertThat(capturedSql)
        .containsExactly(
            "CREATE PUBLICATION dbz_publication FOR TABLE "
                + "\"stockmanagement\".\"table1\", \"stockmanagement\".\"table2\"");
  }

  @Test
  public void shouldAddPublicationWhenPrepareGivenNotAllPublicationExists() throws SQLException {
    // given
    doReturn(conn).when(publicationPreparer).getConn(any());
    HashSet<TableId> existingPublicationTables =
        new HashSet<>(singletonList(new TableId("", "stockmanagement", "table2")));
    given(conn.queryAndMap(any(), any())).willReturn(existingPublicationTables);
    // when
    publicationPreparer.prepare(config);
    // then
    assertThat(capturedSql)
        .containsExactly(
            "ALTER PUBLICATION dbz_publication ADD TABLE \"stockmanagement\".\"table1\"");
  }

  @Test
  public void shouldRemovePublicationWhenPrepareGivenExistingPublicationNotWhitelisted()
      throws SQLException {
    // given
    doReturn(conn).when(publicationPreparer).getConn(any());
    HashSet<TableId> existingPublicationTables =
        new HashSet<>(singletonList(new TableId("", "unkown", "table")));
    given(conn.queryAndMap(any(), any())).willReturn(existingPublicationTables);
    // when
    publicationPreparer.prepare(config);
    // then
    assertThat(capturedSql)
        .containsExactly(
            "ALTER PUBLICATION dbz_publication ADD TABLE "
                + "\"stockmanagement\".\"table1\", \"stockmanagement\".\"table2\"",
            "ALTER PUBLICATION dbz_publication DROP TABLE \"unkown\".\"table\"");
  }

  private void configureConn() throws SQLException {
    given(conn.execute(anyString()))
        .willAnswer(
          (Answer<JdbcConnection>)
            invocation -> {
              String sql = invocation.getArgumentAt(0, String.class);
              capturedSql.add(sql);
              return null;
            });
    given(conn.readAllTableNames(null))
        .willReturn(
          new HashSet<>(
            Arrays.asList(
              new TableId("", "public", "table"),
              new TableId("", "stockmanagement", "table1"),
              new TableId("", "stockmanagement", "table2"))));
  }

  private void configureConfig() {
    ConfigBuilder configBuilder =
        new ConfigBuilder(
            "jdbc:postgresql://localhost:5432/open_lmis?stringtype=unspecified",
            "username",
            "password");
    config =
        configBuilder
            .debeziumConfigBuilder()
            .with("table.include.list", "stockmanagement.*")
            .build();
  }
}
