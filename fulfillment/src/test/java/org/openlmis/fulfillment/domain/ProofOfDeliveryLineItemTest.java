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

package org.openlmis.fulfillment.domain;

import static org.hamcrest.Matchers.startsWith;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_INCORRECT_QUANTITIES;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_INCORRECT_VVM_STATUS;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_MISSING_REASON;
import static org.openlmis.fulfillment.i18n.MessageKeys.MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.fulfillment.ProofOfDeliveryLineItemDataBuilder;
import org.openlmis.fulfillment.domain.naming.VvmStatus;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.testutils.OrderableDataBuilder;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;

public class ProofOfDeliveryLineItemTest {

  private Map<VersionIdentityDto, OrderableDto> orderables = Maps.newHashMap();
  private OrderableDto orderableDto;
  private Map<String, String> extraData = new HashMap<>();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    extraData.put("useVVM", "true");
    orderableDto = new OrderableDataBuilder()
        .withId(UUID.randomUUID())
        .withVersionNumber(1L)
        .withExtraData(extraData)
        .build();

    orderables.put(orderableDto.getIdentity(), orderableDto);
  }

  @Test
  public void shouldThrowExceptionIfQuantityAcceptedIsLessThanZero() {
    exception.expect(ValidationException.class);
    exception.expectMessage(startsWith(MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO));
    new ProofOfDeliveryLineItemDataBuilder()
        .withIncorrectQuantityAccepted()
        .build()
        .validate(null, null);
  }

  @Test
  public void shouldThrowExceptionIfQuantityRejectedIsLessThanZero() {
    exception.expect(ValidationException.class);
    exception.expectMessage(startsWith(MUST_BE_GREATER_THAN_OR_EQUAL_TO_ZERO));
    new ProofOfDeliveryLineItemDataBuilder()
        .withIncorrectQuantityRejected()
        .build()
        .validate(null, null);
  }

  @Test
  public void shouldThrowExceptionIfUseVvmAndStatusIsNull() {
    exception.expect(ValidationException.class);
    exception.expectMessage(startsWith(ERROR_INCORRECT_VVM_STATUS));
    ProofOfDeliveryLineItem lineItem = new ProofOfDeliveryLineItemDataBuilder()
        .withOrderable(orderableDto.getId(), orderableDto.getVersionNumber())
        .withoutVvmStatus()
        .build();
    lineItem.validate(null, orderables);
  }

  @Test
  public void shouldThrowExceptionIfUseVvmAndStatusIsIncorrect() {
    exception.expect(ValidationException.class);
    exception.expectMessage(startsWith(ERROR_INCORRECT_VVM_STATUS));
    new ProofOfDeliveryLineItemDataBuilder()
        .withOrderable(orderableDto.getId(), orderableDto.getVersionNumber())
        .withVvmStatus(VvmStatus.STAGE_4)
        .build()
        .validate(null, orderables);
  }

  @Test
  public void shouldThrowExceptionIfReasonIsNotProvided() {
    exception.expect(ValidationException.class);
    exception.expectMessage(startsWith(ERROR_MISSING_REASON));
    new ProofOfDeliveryLineItemDataBuilder()
        .withOrderable(orderableDto.getId(), orderableDto.getVersionNumber())
        .withoutReason()
        .build()
        .validate(null, orderables);
  }

  @Test
  public void shouldThrowExceptionIfSumIsIncorrect() {
    exception.expect(ValidationException.class);
    exception.expectMessage(startsWith(ERROR_INCORRECT_QUANTITIES));

    ProofOfDeliveryLineItem line = new ProofOfDeliveryLineItemDataBuilder()
        .withOrderable(orderableDto.getId(), orderableDto.getVersionNumber())
        .build();
    // we calculate shipped quantity in this way to make sure that the sum of accepted and
    // rejected quantities will be incorrect
    Long quantityShipped = (long) (line.getQuantityAccepted() + line.getQuantityRejected() + 10);
    line.validate(quantityShipped, orderables);
  }

  @Test
  public void shouldValidate() {
    ProofOfDeliveryLineItem line = new ProofOfDeliveryLineItemDataBuilder()
        .withOrderable(orderableDto.getId(), orderableDto.getVersionNumber())
        .build();
    Long quantityShipped = (long) (line.getQuantityAccepted() + line.getQuantityRejected());
    line.validate(quantityShipped, orderables);
  }

}
