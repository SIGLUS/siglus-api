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

package org.openlmis.fulfillment.web.stockmanagement;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.openlmis.fulfillment.web.stockmanagement.StockEventLineItemDto.QUANTITY_REJECTED;
import static org.openlmis.fulfillment.web.stockmanagement.StockEventLineItemDto.REJECTION_REASON_ID;
import static org.openlmis.fulfillment.web.stockmanagement.StockEventLineItemDto.VVM_STATUS;

import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.fulfillment.domain.naming.VvmStatus;
import org.openlmis.fulfillment.testutils.ToStringTestUtils;

public class StockEventLineItemDtoTest {

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(StockEventLineItemDto.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    StockEventLineItemDto event = new StockEventLineItemDtoDataBuilder().build();
    ToStringTestUtils.verify(StockEventLineItemDto.class, event);
  }

  @Test
  public void shouldAddVvmStatusToExtraData() {
    StockEventLineItemDto event = new StockEventLineItemDtoDataBuilder().build();
    event.setVvmStatus(VvmStatus.STAGE_3);

    assertThat(event.getExtraData(), hasEntry(VVM_STATUS, "STAGE_3"));
  }

  @Test
  public void shouldNotModifyExtraDataIfVvmStatusIsNull() {
    StockEventLineItemDto event = new StockEventLineItemDtoDataBuilder().build();
    event.setVvmStatus(null);

    assertThat(event.getExtraData(), not(hasKey(VVM_STATUS)));
  }

  @Test
  public void shouldAddQuantityRejectedToExtraData() {
    StockEventLineItemDto event = new StockEventLineItemDtoDataBuilder().build();
    event.setQuantityRejected(25);

    assertThat(event.getExtraData(), hasEntry(QUANTITY_REJECTED, "25"));
  }

  @Test
  public void shouldNotModifyExtraDataIfQuantityRejectedIsNull() {
    StockEventLineItemDto event = new StockEventLineItemDtoDataBuilder().build();
    event.setQuantityRejected(null);

    assertThat(event.getExtraData(), not(hasKey(QUANTITY_REJECTED)));
  }

  @Test
  public void shouldAddRejectionReasonIdToExtraData() {
    UUID rejectionReasonId = UUID.randomUUID();
    StockEventLineItemDto event = new StockEventLineItemDtoDataBuilder().build();
    event.setRejectionReasonId(rejectionReasonId);

    assertThat(event.getExtraData(), hasEntry(REJECTION_REASON_ID, rejectionReasonId.toString()));
  }

  @Test
  public void shouldNotModifyExtraDataIfRejectionReasonIdIsNull() {
    StockEventLineItemDto event = new StockEventLineItemDtoDataBuilder().build();
    event.setRejectionReasonId(null);

    assertThat(event.getExtraData(), not(hasKey(REJECTION_REASON_ID)));
  }
}
