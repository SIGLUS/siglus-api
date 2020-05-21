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

package org.openlmis.requisition.validate;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SPECIAL_REASON_NOT_VALID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.StockAdjustmentReason;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.settings.service.ConfigurationSettingService;

@RunWith(MockitoJUnitRunner.class)
public class ReasonsValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ConfigurationSettingService configurationSettingService;

  @InjectMocks
  private ReasonsValidator validator;

  private RequisitionTemplate template = mock(RequisitionTemplate.class);

  private UUID consumedReasonId = UUID.randomUUID();
  private UUID receiptsReasonId = UUID.randomUUID();
  private UUID bbExcessReasonId = UUID.randomUUID();
  private UUID bbInsufficiencyReasonId = UUID.randomUUID();

  @Before
  public void setUp() {
    stubReasonIdInSettingsService();

    when(template.isColumnInTemplateAndDisplayed(anyString())).thenReturn(true);
  }

  @Test
  public void shouldPassWhenAllColumnsAreHiddenOrNotInTemplate() {
    when(template.isColumnInTemplateAndDisplayed(anyString())).thenReturn(false);

    validator.validate(Collections.emptyList(), template);
  }

  @Test
  public void shouldPassWhenAllReasonsExistForAllColumns() {
    validator.validate(prepareReasons(), template);
  }

  @Test
  public void shouldThrowExceptionWhenConsumedReasonNotInReasonList() {
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(ERROR_SPECIAL_REASON_NOT_VALID);

    List<StockAdjustmentReason> reasons = new ArrayList<>(3);
    reasons.add(prepareReason(receiptsReasonId));
    reasons.add(prepareReason(bbExcessReasonId));
    reasons.add(prepareReason(bbInsufficiencyReasonId));

    validator.validate(reasons, template);
  }

  @Test
  public void shouldThrowExceptionWhenReceiptsReasonNotInReasonList() {
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(ERROR_SPECIAL_REASON_NOT_VALID);

    List<StockAdjustmentReason> reasons = new ArrayList<>(3);
    reasons.add(prepareReason(consumedReasonId));
    reasons.add(prepareReason(bbExcessReasonId));
    reasons.add(prepareReason(bbInsufficiencyReasonId));

    validator.validate(reasons, template);
  }

  @Test
  public void shouldThrowExceptionWhenBbExcessReasonReasonNotInReasonList() {
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(ERROR_SPECIAL_REASON_NOT_VALID);

    List<StockAdjustmentReason> reasons = new ArrayList<>(3);
    reasons.add(prepareReason(consumedReasonId));
    reasons.add(prepareReason(receiptsReasonId));
    reasons.add(prepareReason(bbInsufficiencyReasonId));

    validator.validate(reasons, template);
  }

  @Test
  public void shouldThrowExceptionWhenBbInsufficiencyReasonNotInReasonList() {
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(ERROR_SPECIAL_REASON_NOT_VALID);

    List<StockAdjustmentReason> reasons = new ArrayList<>(3);
    reasons.add(prepareReason(consumedReasonId));
    reasons.add(prepareReason(receiptsReasonId));
    reasons.add(prepareReason(bbExcessReasonId));

    validator.validate(reasons, template);
  }

  private void stubReasonIdInSettingsService() {
    when(configurationSettingService.getReasonIdForConsumed())
        .thenReturn(consumedReasonId);
    when(configurationSettingService.getReasonIdForReceipts())
        .thenReturn(receiptsReasonId);
    when(configurationSettingService.getReasonIdForBeginningBalanceExcess())
        .thenReturn(bbExcessReasonId);
    when(configurationSettingService.getReasonIdForBeginningBalanceInsufficiency())
        .thenReturn(bbInsufficiencyReasonId);
  }

  private List<StockAdjustmentReason> prepareReasons() {
    List<StockAdjustmentReason> reasons = new ArrayList<>(4);
    reasons.add(prepareReason(consumedReasonId));
    reasons.add(prepareReason(receiptsReasonId));
    reasons.add(prepareReason(bbExcessReasonId));
    reasons.add(prepareReason(bbInsufficiencyReasonId));
    return reasons;
  }


  private StockAdjustmentReason prepareReason(UUID reasonId) {
    StockAdjustmentReason reason = new StockAdjustmentReason();
    reason.setReasonId(reasonId);
    return reason;
  }

}