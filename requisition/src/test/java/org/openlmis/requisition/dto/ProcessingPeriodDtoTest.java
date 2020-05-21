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

package org.openlmis.requisition.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openlmis.requisition.dto.ProcessingPeriodDto.REPORT_ONLY;

import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.requisition.testutils.ProcessingPeriodDtoDataBuilder;
import org.openlmis.requisition.testutils.ProcessingScheduleDtoDataBuilder;

public class ProcessingPeriodDtoTest extends EqualsContractTest<ProcessingPeriodDto> {

  @Override
  protected Class<ProcessingPeriodDto> getTestClass() {
    return ProcessingPeriodDto.class;
  }

  @Override
  protected void prepare(EqualsVerifier<ProcessingPeriodDto> verifier) {
    verifier
        .withRedefinedSuperclass()
        .withRedefinedSubclass(RequisitionPeriodDto.class);
  }

  @Test
  public void shouldReturnTrueIfReportOnlyExistsAndItIsSetToTrue() {
    ProcessingPeriodDto period = prepareForReportOnlyTest(true, "true");
    assertThat(period.isReportOnly()).isTrue();
  }

  @Test
  public void shouldReturnFalseIfReportOnlyExistsAndItIsSetToFalse() {
    ProcessingPeriodDto period = prepareForReportOnlyTest(true, "false");
    assertThat(period.isReportOnly()).isFalse();
  }

  @Test
  public void shouldReturnFalseIfReportOnlyExistsAndItHasInvalidValue() {
    ProcessingPeriodDto period = prepareForReportOnlyTest(true, "some-invalid-value");
    assertThat(period.isReportOnly()).isFalse();
  }

  @Test
  public void shouldReturnFalseIfReportOnlyDoesNotExist() {
    ProcessingPeriodDto period = prepareForReportOnlyTest(true, null);
    assertThat(period.isReportOnly()).isFalse();
  }

  @Test
  public void shouldReturnFalseIfExtraDataDoesNotExist() {
    ProcessingPeriodDto period = prepareForReportOnlyTest(false, null);
    assertThat(period.isReportOnly()).isFalse();
  }

  private ProcessingPeriodDto prepareForReportOnlyTest(boolean extraData, String reportOnly) {
    ProcessingPeriodDto period = new ProcessingPeriodDtoDataBuilder()
        .withId(UUID.randomUUID())
        .withName("period")
        .withStartDate(LocalDate.now())
        .withEndDate(LocalDate.now())
        .withProcessingSchedule(new ProcessingScheduleDtoDataBuilder().buildAsDto())
        .withDescription("desc")
        .withDurationInMonths(3)
        .buildAsDto();

    if (extraData) {
      period.setExtraData(Maps.newHashMap());
    }

    if (null != reportOnly) {
      period.getExtraData().put(REPORT_ONLY, reportOnly);
    }

    return period;
  }
}
