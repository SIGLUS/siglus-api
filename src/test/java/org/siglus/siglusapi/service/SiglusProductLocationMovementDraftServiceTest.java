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

package org.siglus.siglusapi.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MOVEMENT_DRAFT_EXISTS;

import java.util.Collections;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.ProductLocationMovementDraft;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftDto;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.ProductLocationMovementDraftRepository;
import org.siglus.siglusapi.validator.StockManagementDraftValidator;

@RunWith(MockitoJUnitRunner.class)
public class SiglusProductLocationMovementDraftServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusProductLocationMovementDraftService service;

  @Mock
  private ProductLocationMovementDraftRepository productLocationMovementDraftRepository;

  @Mock
  private StockManagementDraftValidator stockManagementDraftValidator;

  private final UUID programId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final ProductLocationMovementDraftDto productLocationMovementDraftDto = ProductLocationMovementDraftDto
      .builder().programId(programId).facilityId(facilityId).build();
  private final ProductLocationMovementDraft productLocationMovementDraft = ProductLocationMovementDraft
      .builder().programId(programId).facilityId(facilityId).build();

  @Test
  public void shouldCreateEmptyProductLocationMovementDraft() {

    doNothing().when(stockManagementDraftValidator)
        .validateEmptyStockMovementDraft(productLocationMovementDraftDto);
    when(productLocationMovementDraftRepository.findByProgramIdAndFacilityId(programId, facilityId)).thenReturn(
        Collections.emptyList());
    when(productLocationMovementDraftRepository.save(any(ProductLocationMovementDraft.class)))
        .thenReturn(productLocationMovementDraft);

    ProductLocationMovementDraftDto emptyProductLocationMovementDraft = service
        .createEmptyProductLocationMovementDraft(productLocationMovementDraftDto);

    assertThat(emptyProductLocationMovementDraft.getProgramId()).isEqualTo(programId);
    assertThat(emptyProductLocationMovementDraft.getFacilityId()).isEqualTo(facilityId);
  }

  @Test
  public void shouldThrowExceptionWhenProductLocationMovementDraftExists() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MOVEMENT_DRAFT_EXISTS));

    doNothing().when(stockManagementDraftValidator)
        .validateEmptyStockMovementDraft(productLocationMovementDraftDto);
    when(productLocationMovementDraftRepository.findByProgramIdAndFacilityId(programId, facilityId))
        .thenReturn(newArrayList(productLocationMovementDraft));

    service.createEmptyProductLocationMovementDraft(productLocationMovementDraftDto);
  }
}