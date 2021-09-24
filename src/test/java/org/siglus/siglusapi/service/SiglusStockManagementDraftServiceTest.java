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
import static org.apache.commons.lang3.RandomUtils.nextBoolean;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_PROGRAM_NOT_SUPPORTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.service.PermissionService;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.siglus.siglusapi.validator.StockManagementDraftValidator;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unused")
public class SiglusStockManagementDraftServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusStockManagementDraftService siglusStockManagementDraftService;

  @Mock
  private ActiveDraftValidator draftValidator;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Mock
  private StockManagementDraftValidator stockManagementDraftValidator;

  @Mock
  private SupportedProgramsHelper supportedProgramsHelper;

  @Mock
  private PermissionService permissionService;

  private final UUID id = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  private final Boolean isDraft = nextBoolean();

  private final String draftType = "draftType";

  @Before
  public void setup() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
  }

  @Test
  public void shouldThrowExceptionWhenCreateNewDraftIfDraftExists() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS));

    StockManagementDraft draft = StockManagementDraft.builder().build();
    when(stockManagementDraftRepository.findByProgramIdAndFacilityIdAndIsDraftAndDraftType(programId,
        facilityId, true, draftType)).thenReturn(newArrayList(draft));
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .programId(programId).facilityId(facilityId).draftType(draftType).build();

    siglusStockManagementDraftService.createNewDraft(draftDto);
  }

  @Test
  public void shouldIsDraftBeTrueWhenCreateNewDraft() {
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .programId(programId).facilityId(facilityId).draftType(draftType).build();
    StockManagementDraft draft = StockManagementDraft.createEmptyDraft(draftDto);
    when(stockManagementDraftRepository.save(draft)).thenReturn(draft);

    draftDto = siglusStockManagementDraftService.createNewDraft(draftDto);

    assertTrue(draftDto.getIsDraft());
  }

  @Test
  public void shouldIsDraftBeTrueWhenSaveDraft() {
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .programId(programId).facilityId(facilityId).draftType(draftType).build();
    StockManagementDraft draft = StockManagementDraft.createStockManagementDraft(draftDto, true);
    when(stockManagementDraftRepository.save(draft)).thenReturn(draft);

    draftDto = siglusStockManagementDraftService.saveDraft(draftDto, id);

    assertTrue(draftDto.getIsDraft());
  }

  @Test
  public void shouldCallRepositoryWhenFindStockManagementDraft() {
    siglusStockManagementDraftService.findStockManagementDraft(programId, draftType, isDraft);

    verify(draftValidator).validateIsDraft(isDraft);
    verify(stockManagementDraftRepository).findByProgramIdAndFacilityIdAndIsDraftAndDraftType(programId,
        facilityId, isDraft, draftType);
  }

  @Test
  public void shouldThrowExceptionWhenDeleteNotExistsDraft() {
    exception.expect(ResourceNotFoundException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND));

    siglusStockManagementDraftService.deleteStockManagementDraft(id);
  }

  @Test
  public void shouldThrowProgramExceptionWhenDeleteNotPermission() {
    // then
    exception.expect(PermissionMessageException.class);
    exception.expectMessage(containsString(ERROR_PROGRAM_NOT_SUPPORTED));

    // given
    StockManagementDraft draft = StockManagementDraft.builder().build();
    when(stockManagementDraftRepository.findOne(id)).thenReturn(draft);
    when(supportedProgramsHelper.findUserSupportedPrograms()).thenReturn(null);

    // when
    siglusStockManagementDraftService.deleteStockManagementDraft(id);
  }

  @Test
  public void shouldCallRepositoryWhenDeleteStockManagementDraftById() {
    // given
    StockManagementDraft draft = StockManagementDraft.builder().build();
    when(stockManagementDraftRepository.findOne(id)).thenReturn(draft);
    Set<UUID> supportedPrograms = new HashSet<>();
    supportedPrograms.add(UUID.randomUUID());
    when(supportedProgramsHelper.findUserSupportedPrograms()).thenReturn(supportedPrograms);

    // when
    siglusStockManagementDraftService.deleteStockManagementDraft(id);

    //then
    verify(stockManagementDraftRepository).delete(draft);
  }

  @Test
  public void shouldCallRepositoryWhenDeleteStockManagementDraftByStockEventDto() {
    StockManagementDraft draft = StockManagementDraft.builder().build();
    final ArrayList<StockManagementDraft> drafts = newArrayList(draft);
    when(stockManagementDraftRepository.findByProgramIdAndFacilityIdAndIsDraftAndDraftType(programId,
        facilityId, true, draftType)).thenReturn(drafts);
    StockEventDto stockEventDto = StockEventDto.builder().programId(programId).facilityId(facilityId)
        .type(draftType).build();

    siglusStockManagementDraftService.deleteStockManagementDraft(stockEventDto);

    verify(stockManagementDraftRepository).delete(drafts);
  }
}
