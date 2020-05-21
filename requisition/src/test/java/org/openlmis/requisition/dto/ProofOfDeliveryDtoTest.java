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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.openlmis.requisition.dto.ProofOfDeliveryStatus.CONFIRMED;
import static org.openlmis.requisition.dto.ProofOfDeliveryStatus.INITIATED;

import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.requisition.testutils.DtoGenerator;

public class ProofOfDeliveryDtoTest {

  @Test
  public void shouldBeSubmittedIfOrderHasCorrectStatus() {
    ProofOfDeliveryDto pod = ProofOfDeliveryDto.builder().status(CONFIRMED).build();
    assertThat(pod.isSubmitted(), is(true));
  }

  @Test
  public void shouldBeNotSubmittedIfOrderHasIncorrectStatus() {
    ProofOfDeliveryDto pod = ProofOfDeliveryDto.builder().status(INITIATED).build();
    assertThat(pod.isSubmitted(), is(false));
  }

  @Test
  public void shouldFindLineByProductId() {
    ProofOfDeliveryDto pod = ProofOfDeliveryDto
        .builder()
        .lineItems(DtoGenerator.of(ProofOfDeliveryLineItemDto.class, 2))
        .build();

    ProofOfDeliveryLineItemDto line1 = pod.getLineItems().get(0);
    ProofOfDeliveryLineItemDto line2 = pod.getLineItems().get(1);

    UUID productId1 = line1.getOrderable().getId();
    UUID productId2 = line2.getOrderable().getId();

    assertThat(pod.findLineByProductId(UUID.randomUUID()), is(nullValue()));
    assertThat(pod.findLineByProductId(productId1).getId(), is(equalTo(line1.getId())));
    assertThat(pod.findLineByProductId(productId2).getId(), is(equalTo(line2.getId())));
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(ProofOfDeliveryDto.class)
        .withRedefinedSuperclass()
        .suppress(Warning.NONFINAL_FIELDS) // fields in DTO cannot be final
        .verify();
  }
}
