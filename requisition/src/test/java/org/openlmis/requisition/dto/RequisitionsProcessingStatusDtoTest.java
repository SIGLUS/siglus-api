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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.testutils.ApproveRequisitionDtoDataBuilder;
import org.openlmis.requisition.testutils.ApproveRequisitionLineItemDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.RequisitionsProcessingStatusDtoDataBuilder;

@RunWith(MockitoJUnitRunner.class)
public class RequisitionsProcessingStatusDtoTest {
  private OrderableDto orderable1;
  private OrderableDto orderable2;
  private OrderableDto orderable3;

  private ApproveRequisitionDto requisition1;
  private ApproveRequisitionDto requisition2;
  private ApproveRequisitionDto requisition3;

  private RequisitionsProcessingStatusDto processingStatus;

  @Before
  public void setUp() {
    orderable1 = new OrderableDtoDataBuilder().withId(UUID.randomUUID()).buildAsDto();
    orderable2 = new OrderableDtoDataBuilder().withId(UUID.randomUUID()).buildAsDto();
    orderable3 = new OrderableDtoDataBuilder().withId(UUID.randomUUID()).buildAsDto();

    requisition1 = create(UUID.randomUUID(),
        new OrderableDtoWrapper(orderable1, false),
        new OrderableDtoWrapper(orderable2, true),
        new OrderableDtoWrapper(orderable3, true)
    );

    requisition2 = create(UUID.randomUUID(),
        new OrderableDtoWrapper(orderable1, true),
        new OrderableDtoWrapper(orderable2, false),
        new OrderableDtoWrapper(orderable3, true)
    );
    requisition3 = create(UUID.randomUUID(),
        new OrderableDtoWrapper(orderable1, false),
        new OrderableDtoWrapper(orderable2, false),
        new OrderableDtoWrapper(orderable3, true)
    );

    Set<ApproveRequisitionDto> approveRequisitionDtos = new HashSet<>(
        Arrays.asList(requisition1, requisition2, requisition3));

    processingStatus = new RequisitionsProcessingStatusDtoDataBuilder()
        .withProcessedRequisitions(approveRequisitionDtos)
        .buildAsDto();
  }

  @Test
  public void shouldRemoveLineItemsIfProductIsSkippedInAllRequisitions() {
    processingStatus.removeSkippedProducts();

    Set<ApproveRequisitionDto> requisitions = processingStatus.getRequisitionDtos();

    assertFalse(requisitions.isEmpty());
    assertEquals(3, requisitions.size());

    for (ApproveRequisitionDto requisition : requisitions) {
      List<ApproveRequisitionLineItemDto> lineItems = requisition.getRequisitionLineItems();

      assertEquals(2, lineItems.size());
      assertEquals(lineItems.get(0).getOrderable().getId(), orderable1.getId());
      assertEquals(lineItems.get(1).getOrderable().getId(), orderable2.getId());
    }
  }

  private ApproveRequisitionDto create(UUID key, OrderableDtoWrapper... orderableDtoWrapers) {
    ApproveRequisitionDto dto = new ApproveRequisitionDtoDataBuilder().withId(key).buildAsDto();

    List<ApproveRequisitionLineItemDto> items =
        Arrays.asList(orderableDtoWrapers)
            .stream()
            .map(w -> new ApproveRequisitionLineItemDtoDataBuilder()
                .withOrderable(w.getOrderable())
                .withSkippedFlag(w.isSkipped())
                .buildAsDto())
            .collect(Collectors.toList());

    dto.setRequisitionLineItems(items);

    return dto;
  }

  @Getter
  @AllArgsConstructor
  private class OrderableDtoWrapper {
    private OrderableDto orderable;
    private boolean skipped;
  }

}