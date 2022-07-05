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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_PROGRAM_NOT_SUPPORTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_MORE_THAN_TEN;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.service.PermissionService;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementInitialDraft;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.StockManagementInitialDraftDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.repository.StockManagementInitialDraftsRepository;
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
  private SiglusValidSourceDestinationService siglusValidSourceDestinationService;

  @Mock
  private ActiveDraftValidator draftValidator;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Mock
  private StockManagementInitialDraftsRepository stockManagementInitialDraftsRepository;

  @Mock
  private StockManagementDraftValidator stockManagementDraftValidator;

  @Mock
  private SupportedProgramsHelper supportedProgramsHelper;

  @Mock
  private PermissionService permissionService;

  private final UUID id = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  private final UUID initialDraftId = UUID.randomUUID();

  private final UUID destinationId = UUID.randomUUID();

  private final Boolean isDraft = nextBoolean();

  private final String draftType = "draftType";

  private final StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
      .programId(programId).facilityId(facilityId).draftType(draftType).build();

  @Before
  public void setup() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
  }

  /**
   * in order to support multi-user, the original logic has changed, this test is not applicable.
   * should delete when everything ok
   */
  @Ignore
  @Test
  public void shouldThrowExceptionWhenCreateNewDraftIfDraftExists() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS));

    StockManagementDraft draft = StockManagementDraft.builder().build();
    when(
        stockManagementDraftRepository.findByProgramIdAndFacilityIdAndIsDraftAndDraftType(programId,
            facilityId, true, draftType)).thenReturn(newArrayList(draft));

    siglusStockManagementDraftService.createNewDraft(draftDto);
  }

  @Test
  public void shouldIsDraftBeTrueWhenCreateNewDraft() {
    StockManagementDraft draft = StockManagementDraft.createEmptyDraft(draftDto);
    when(stockManagementDraftRepository.save(draft)).thenReturn(draft);

    StockManagementDraftDto newDraft = siglusStockManagementDraftService.createNewDraft(draftDto);

    assertTrue(newDraft.getIsDraft());
  }

  @Test
  public void shouldUpdateDraftStatusWhenSaveDraft() {
    StockManagementDraft foundDraft = StockManagementDraft.builder()
        .initialDraftId(initialDraftId)
        .operator("operator-1")
        .status(PhysicalInventorySubDraftEnum.NOT_YET_STARTED)
        .build();

    when(stockManagementDraftRepository.findOne(id)).thenReturn(foundDraft);

    StockManagementDraft stockManagementDraft = siglusStockManagementDraftService
        .setNewAttributesInOriginalDraft(draftDto, id);
    when(stockManagementDraftRepository.save(stockManagementDraft))
        .thenReturn(stockManagementDraft);

    StockManagementDraftDto updatedDraftDto = siglusStockManagementDraftService
        .updateDraft(draftDto, id);

    assertThat(updatedDraftDto.getStatus())
        .isEqualTo(PhysicalInventorySubDraftEnum.DRAFT);
    assertThat(updatedDraftDto.getInitialDraftId()).isEqualTo(initialDraftId);
  }

  @Test
  public void shouldCallRepositoryWhenFindStockManagementDraft() {
    siglusStockManagementDraftService.findStockManagementDraft(programId, draftType, isDraft);

    verify(draftValidator).validateIsDraft(isDraft);
    verify(stockManagementDraftRepository)
        .findByProgramIdAndFacilityIdAndIsDraftAndDraftType(programId,
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
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(null);

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
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(supportedPrograms);

    // when
    siglusStockManagementDraftService.deleteStockManagementDraft(id);

    //then
    verify(stockManagementDraftRepository).delete(draft);
  }

  @Test
  public void shouldCallRepositoryWhenDeleteStockManagementDraftByStockEventDto() {
    StockManagementDraft draft = StockManagementDraft.builder().build();
    final ArrayList<StockManagementDraft> drafts = newArrayList(draft);
    when(
        stockManagementDraftRepository.findByProgramIdAndFacilityIdAndIsDraftAndDraftType(programId,
            facilityId, true, draftType)).thenReturn(drafts);
    StockEventDto stockEventDto = StockEventDto.builder().programId(programId)
        .facilityId(facilityId)
        .type(draftType).build();

    siglusStockManagementDraftService.deleteStockManagementDraft(stockEventDto);

    verify(stockManagementDraftRepository).delete(drafts);
  }

  @Test
  public void shouldThrowExceptionWhenDraftsMoreThan10() {
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_MORE_THAN_TEN));

    draftDto.setInitialDraftId(initialDraftId);
    StockManagementDraft draft = StockManagementDraft.builder().build();
    ArrayList<StockManagementDraft> stockManagementDrafts = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      stockManagementDrafts.add(draft);
    }
    when(stockManagementInitialDraftsRepository.findOne(initialDraftId))
        .thenReturn(new StockManagementInitialDraft());
    when(stockManagementDraftRepository.findByInitialDraftId(initialDraftId))
        .thenReturn(stockManagementDrafts);

    siglusStockManagementDraftService.createNewDraft(draftDto);
  }

  @Test
  public void shouldFindStockManagementDrafts() {
    StockManagementDraft draft = StockManagementDraft.builder().build();
    ArrayList<StockManagementDraft> stockManagementDrafts = newArrayList(draft);
    doNothing().when(draftValidator).validateInitialDraftId(initialDraftId);
    when(stockManagementDraftRepository
        .findByInitialDraftId(initialDraftId)).thenReturn(stockManagementDrafts);

    List<StockManagementDraftDto> foundDrafts = siglusStockManagementDraftService
        .findStockManagementDrafts(initialDraftId);

    assertThat(foundDrafts.size()).isEqualTo(1);
  }

  @Test
  public void shouldFindStockManagementIssueInitialDraft() {
    ValidSourceDestinationDto validSourceDestinationDto = new ValidSourceDestinationDto();
    validSourceDestinationDto.setId(destinationId);
    validSourceDestinationDto.setName("issue-location");

    Collection<ValidSourceDestinationDto> validSourceDestinationDtos = new ArrayList<>(
        Collections.emptyList());
    validSourceDestinationDtos.add(validSourceDestinationDto);

    StockManagementInitialDraft foundInitialDraft = StockManagementInitialDraft.builder()
        .draftType("issue")
        .destinationId(destinationId)
        .build();
    ArrayList<StockManagementInitialDraft> stockManagementInitialDrafts = newArrayList(
        foundInitialDraft);

    doNothing().when(draftValidator).validateProgramId(programId);
    doNothing().when(draftValidator).validateFacilityId(facilityId);
    doNothing().when(draftValidator).validateDraftType(draftType);

    when(stockManagementInitialDraftsRepository
        .findByProgramIdAndFacilityIdAndDraftType(programId, facilityId, "issue"))
        .thenReturn(stockManagementInitialDrafts);

    when(siglusValidSourceDestinationService.findDestinationsForAllProducts(facilityId))
        .thenReturn(validSourceDestinationDtos);

    StockManagementInitialDraftDto stockManagementInitialDraft = siglusStockManagementDraftService
        .findStockManagementInitialDraft(programId, "issue");

    assertThat(stockManagementInitialDraft.getDestinationName()).isEqualTo("issue-location");
  }

  @Test
  public void shouldReturnEmptyInitialDraftWhenNotfindInitialDraft() {

    doNothing().when(draftValidator).validateProgramId(programId);
    doNothing().when(draftValidator).validateFacilityId(facilityId);
    doNothing().when(draftValidator).validateDraftType(draftType);

    when(stockManagementInitialDraftsRepository
        .findByProgramIdAndFacilityIdAndDraftType(programId, facilityId, "issue"))
        .thenReturn(Collections.emptyList());

    StockManagementInitialDraftDto initialDraft = siglusStockManagementDraftService
        .findStockManagementInitialDraft(programId, "issue");

    assertThat(initialDraft.getDestinationName()).isNull();
    assertThat(initialDraft.getFacilityId()).isNull();
    assertThat(initialDraft.getDraftType()).isNull();
    assertThat(initialDraft.getProgramId()).isNull();

  }
}
