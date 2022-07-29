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

package org.siglus.siglusapi.util;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ISSUE_CONFLICT_SUB_DRAFT;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_SUB_DRAFT_SAME_ORDERABLE_ID_WITH_LOT_CODE;

import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.StockManagementDraftLineItemDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class CheckConflictOrderableInSubDraftsServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private CheckConflictOrderableInSubDraftsService checkConflictOrderableInSubDraftsService;

  @Mock
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Mock
  private SiglusOrderableReferenceDataService siglusOrderableReferenceDataService;

  private final UUID draftId1 = UUID.randomUUID();
  private final UUID draftId2 = UUID.randomUUID();
  private final UUID orderableId1 = UUID.randomUUID();
  private final UUID orderableId2 = UUID.randomUUID();
  private final UUID initialDraftId = UUID.randomUUID();

  private final StockManagementDraftDto draftDto = StockManagementDraftDto
      .builder()
      .id(draftId1)
      .build();

  private final StockManagementDraft currentDraft = new StockManagementDraft();

  private final StockManagementDraftLineItemDto itemDto1 = StockManagementDraftLineItemDto.builder()
      .orderableId(orderableId1).build();
  private final StockManagementDraftLineItemDto itemDto2 = StockManagementDraftLineItemDto.builder()
      .orderableId(orderableId2).build();

  private final StockManagementDraftLineItem item1 = StockManagementDraftLineItem.builder()
      .orderableId(orderableId1).build();
  private final StockManagementDraftLineItem item2 = StockManagementDraftLineItem.builder()
      .orderableId(orderableId2).build();

  @Test
  public void shouldGetConflictSubDraftWithDetails() {
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_ISSUE_CONFLICT_SUB_DRAFT));

    currentDraft.setId(draftId1);
    currentDraft.setInitialDraftId(initialDraftId);

    OrderableDto orderableDto1 = new OrderableDto();
    orderableDto1.setId(orderableId1);
    orderableDto1.setFullProductName("product-1");
    orderableDto1.setProductCode("code-1");

    OrderableDto orderableDto2 = new OrderableDto();
    orderableDto2.setId(orderableId2);
    orderableDto2.setFullProductName("product-2");
    orderableDto2.setProductCode("code-2");

    draftDto.setLineItems(newArrayList(itemDto1, itemDto2));

    StockManagementDraft subDraft = new StockManagementDraft();
    subDraft.setId(draftId2);
    subDraft.setDraftNumber(2);
    subDraft.setInitialDraftId(initialDraftId);
    subDraft.setLineItems(newArrayList(item1, item2));

    when(stockManagementDraftRepository.findOne(draftDto.getId())).thenReturn(currentDraft);

    when(stockManagementDraftRepository.findByInitialDraftId(initialDraftId))
        .thenReturn(newArrayList(currentDraft, subDraft));

    when(siglusOrderableReferenceDataService.findByIds(newArrayList(orderableId1, orderableId2)))
        .thenReturn(newArrayList(orderableDto1, orderableDto2));

    checkConflictOrderableInSubDraftsService.checkConflictOrderableBetweenSubDrafts(draftDto);
  }

  @Test
  public void shouldThrowExceptionWhenLineItemsHaveSameOrderableIdAndLotCode() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MANAGEMENT_SUB_DRAFT_SAME_ORDERABLE_ID_WITH_LOT_CODE));

    StockManagementDraftLineItemDto stockManagementDraftLineItemDto1 = new StockManagementDraftLineItemDto();
    stockManagementDraftLineItemDto1.setOrderableId(orderableId1);
    stockManagementDraftLineItemDto1.setLotCode("LotCode-1");

    StockManagementDraftLineItemDto stockManagementDraftLineItemDto2 = new StockManagementDraftLineItemDto();
    stockManagementDraftLineItemDto2.setOrderableId(orderableId1);
    stockManagementDraftLineItemDto2.setLotCode("LotCode-1");

    StockManagementDraftDto subDraftDto = new StockManagementDraftDto();
    subDraftDto.setDraftType(FieldConstants.RECEIVE);
    subDraftDto.setLineItems(newArrayList(stockManagementDraftLineItemDto1, stockManagementDraftLineItemDto2));

    checkConflictOrderableInSubDraftsService.checkConflictOrderableAndLotInSubDraft(subDraftDto);
  }
}