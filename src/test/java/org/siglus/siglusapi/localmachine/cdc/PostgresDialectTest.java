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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.debezium.time.Date;
import io.debezium.time.MicroTime;
import io.debezium.time.MicroTimestamp;
import io.debezium.time.Timestamp;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.TimeZone;
import org.apache.kafka.connect.data.Schema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.postgresql.jdbc.PgConnection;

@RunWith(MockitoJUnitRunner.class)
public class PostgresDialectTest {

  private static final int VALUE = 123;
  private final PostgresDialect postgresDialect = mock(PostgresDialect.class);
  private final TimeZone utc = TimeZone.getTimeZone("UTC");

  @Before
  public void setup() throws SQLException {
    given(postgresDialect.maybeBindLogical(any(), anyInt(), any(), any())).willCallRealMethod();
    given(postgresDialect.timeZone()).willReturn(utc);
  }

  @Test
  public void shouldCommitWhenConfigureConnAsReplicaRoleGivenConnIsNotAutoCommit()
      throws SQLException {
    // given
    doCallRealMethod().when(postgresDialect).configureConnAsReplicaRole(any());
    PgConnection conn = mock(PgConnection.class);
    given(conn.getAutoCommit()).willReturn(Boolean.FALSE);
    // when
    postgresDialect.configureConnAsReplicaRole(conn);
    // then
    verify(conn, times(1)).commit();
  }

  @Test(expected = RuntimeException.class)
  public void shouldThrowWhenBindLogicalValueGivenSchemaIsOtherTimeTypeOfDebezium()
      throws SQLException {
    verifyLogicalBind(MicroTime.schema(), VALUE);
  }

  @Test
  public void shouldBoundWhenBindLogicalValueGivenSchemaIsDateAndValueIsInt() throws SQLException {
    assertThat(verifyLogicalBind(Date.schema(), VALUE)).isTrue();
  }

  @Test
  public void shouldBoundWhenBindLogicalValueGivenSchemaIsDateAndValueIsLong() throws SQLException {
    assertThat(verifyLogicalBind(Date.schema(), 123L)).isTrue();
  }

  @Test
  public void shouldBoundWhenBindLogicalValueGivenSchemaIsTimestampAndValueIsLong()
      throws SQLException {
    assertThat(verifyLogicalBind(Timestamp.schema(), 123L)).isTrue();
  }

  @Test
  public void shouldBoundWhenBindLogicalValueGivenSchemaIsTimestampAndValueIsInt()
      throws SQLException {
    assertThat(verifyLogicalBind(Timestamp.schema(), VALUE)).isTrue();
  }

  @Test
  public void shouldBoundWhenBindLogicalValueGivenSchemaIsMicroTimestampAndValueIsLong()
      throws SQLException {
    assertThat(verifyLogicalBind(MicroTimestamp.schema(), 123L)).isTrue();
  }

  @Test
  public void shouldBoundWhenBindLogicalValueGivenSchemaIsMicroTimestampAndValueIsInt()
      throws SQLException {
    assertThat(verifyLogicalBind(MicroTimestamp.schema(), VALUE)).isTrue();
  }

  @Test
  public void shouldReturnFalseWhenBindLogicalValueGivenSchemaIsNull() throws SQLException {
    assertThat(verifyLogicalBind(null, null)).isFalse();
  }

  private boolean verifyLogicalBind(Schema schema, Object value) throws SQLException {
    int index = 1;
    return postgresDialect.maybeBindLogical(mock(PreparedStatement.class), index, schema, value);
  }
}
