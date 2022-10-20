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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang.BooleanUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.utils.Pagination;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.SiglusReportType;
import org.siglus.siglusapi.dto.FacilityDeviceDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilitySearchParamDto;
import org.siglus.siglusapi.dto.FacilitySearchResultDto;
import org.siglus.siglusapi.dto.SiglusFacilityDto;
import org.siglus.siglusapi.dto.SiglusReportTypeDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.enums.FacilityDeviceTypeEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.repository.ActivationCodeRepository;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.LocationDraftRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
import org.siglus.siglusapi.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.repository.StockCardLocationMovementLineItemRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.CsvValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusAdministrationsServiceTest {

  @InjectMocks
  private SiglusAdministrationsService siglusAdministrationsService;
  @Mock
  private AppInfoRepository appInfoRepository;
  @Mock
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  @Mock
  private FacilityExtensionRepository facilityExtensionRepository;
  @Mock
  private FacilityLocationsRepository locationManagementRepository;
  @Mock
  private AndroidHelper androidHelper;
  @Mock
  private CsvValidator csvValidator;
  @Mock
  private StockCardRepository stockCardRepository;
  @Mock
  private SiglusReportTypeRepository siglusReportTypeRepository;
  @Mock
  private CalculatedStockOnHandRepository calculatedStockOnHandRepository;
  @Mock
  private SiglusProcessingPeriodService siglusProcessingPeriodService;
  @Mock
  private StockCardLocationMovementLineItemRepository stockCardLocationMovementLineItemRepository;
  @Mock
  private CalculatedStockOnHandByLocationRepository calculatedStocksOnHandLocationsRepository;
  @Mock
  private StockCardLineItemRepository stockCardLineItemRepository;
  @Mock
  private StockCardExtensionRepository stockCardExtensionRepository;
  @Mock
  private SiglusAuthenticationHelper authenticationHelper;
  @Mock
  private OrderableRepository orderableRepository;
  @Mock
  private LocationDraftRepository locationDraftRepository;
  @Mock
  private AgentInfoRepository agentInfoRepository;
  @Mock
  public ActivationCodeRepository activationCodeRepository;
  @Mock
  public Machine machine;
  @Rule
  public ExpectedException exception = ExpectedException.none();
  private static final UUID facilityId = UUID.randomUUID();
  private static final UUID device1 = UUID.randomUUID();
  private static final UUID device2 = UUID.randomUUID();
  private static final UUID device3 = UUID.randomUUID();
  private static final UUID stockCardId = UUID.randomUUID();
  private static final UUID userId = UUID.randomUUID();
  private static final UUID orderableId = UUID.randomUUID();
  private static final String productCode = "04F06W";
  private static final String facilityCode = "01100122";
  private static final String Name = "A. Alimenticios";
  private static final List<FacilityDto> content = new ArrayList<>();
  private static final List<FacilityLocations> locationManagementList = new ArrayList<>();
  private static final Pageable pageable = new PageRequest(0, 3);
  private static int isAndroid = 0;
  private static final String PROGRAM_CODE = "VIA";
  private static final String LOCATION_CODE = "AA25A";
  private static final String AREA = "Armazem Principal";
  private static final String ZONE = "A";
  private static final String RACK = "A";
  private static final String BARCODE = "AA25%";
  private static final String BIN = "25";
  private static final String LEVEL = "A";
  private static final String csvInput =
      "Código de localização,Área,Zona,Prateleira,Código de barras,Caixa,Nível\n"
          + "AA25A,Armazem Principal,A,A,AA25%,25,A\n";

  @Test
  public void searchForFacilitiesWithIsAndroid() {
    // given
    FacilitySearchParamDto facilitySearchParamDto = mockFacilitySearchParamDto();
    when(siglusFacilityReferenceDataService.searchAllFacilities(facilitySearchParamDto, pageable))
        .thenReturn(mockFacilityDtoPage());
    when(facilityExtensionRepository.findByFacilityId(device1)).thenReturn(
        mockFacilityExtension(device1, true, false, false));
    when(facilityExtensionRepository.findByFacilityId(device2)).thenReturn(
        mockFacilityExtension(device2, false, false, false));
    when(facilityExtensionRepository.findByFacilityId(device3)).thenReturn(null);

    // when
    Page<FacilitySearchResultDto> facilitySearchDtoPage = siglusAdministrationsService
        .searchForFacilities(facilitySearchParamDto, pageable);
    facilitySearchDtoPage.getContent().forEach(eachFacilityDto -> {
      if (BooleanUtils.isTrue(eachFacilityDto.getIsAndroidDevice())) {
        isAndroid++;
      }
    });

    //then
    assertEquals(1, isAndroid);
  }

  @Test
  public void deleteAndroidInfoByFacilityId() {
    // given
    AppInfo appInfo = mockAppInfo();
    when(appInfoRepository.findByFacilityCode(facilityCode)).thenReturn(appInfo);

    // when
    siglusAdministrationsService.eraseDeviceInfoByFacilityId(appInfo.getFacilityCode());

    //then
    verify(appInfoRepository, times(1)).deleteByFacilityCode(appInfo.getFacilityCode());
  }

  @Test
  public void deleteWhenAndroidFacilityNeverLogin() {
    // given
    AppInfo appInfo = mockAppInfo();
    when(appInfoRepository.findByFacilityCode(facilityCode)).thenReturn(null);

    // when
    siglusAdministrationsService.eraseDeviceInfoByFacilityId(appInfo.getFacilityCode());

    // then
    verify(appInfoRepository, times(0)).deleteByFacilityCode(facilityCode);
  }

  @Test
  public void shouldSetPreviousPeriodStartDateWhenUserHasRequisition() {
    // given
    LocalDate date = LocalDate.of(2022, 1, 1);
    when(siglusReportTypeRepository.findByFacilityId(facilityId))
        .thenReturn(Arrays.asList(mockReportType()));
    when(siglusProcessingPeriodService.getPreviousPeriodStartDateSinceInitiate(PROGRAM_CODE, facilityId))
        .thenReturn(date);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto());
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(machine.isOnlineWeb()).thenReturn(true);

    // when
    FacilitySearchResultDto facility = siglusAdministrationsService.getFacility(facilityId);

    // then
    assertEquals(date, facility.getReportTypes().get(0).getPreviousPeriodStartDateSinceRecentSubmit());
  }

  @Test
  public void shouldGetFacilityInfoWhenFacilityExistsAndFacilityExtensionIsNull() {
    // given
    when(siglusReportTypeRepository.findByFacilityId(facilityId))
        .thenReturn(Collections.emptyList());
    when(siglusProcessingPeriodService.getPreviousPeriodStartDateSinceInitiate(null, facilityId))
        .thenReturn(null);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto());
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(machine.isOnlineWeb()).thenReturn(true);

    // when
    FacilitySearchResultDto facility = siglusAdministrationsService.getFacility(facilityId);

    // then
    assertFalse(facility.getEnableLocationManagement());
  }

  @Test
  public void shouldGetFacilityInfoWhenFacilityExistsAndFacilityExtensionIsNotNull() {
    // given
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto());
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(
        mockFacilityExtension(facilityId, false, false, false));
    when(machine.isOnlineWeb()).thenReturn(true);

    // when
    FacilitySearchResultDto facility = siglusAdministrationsService.getFacility(facilityId);

    // then
    assertFalse(facility.getEnableLocationManagement());
  }

  @Test
  public void shouldGetFacilityInfoWhenFacilityNotExists() {
    // given
    exception.expect(NotFoundException.class);
    exception.expectMessage("Resources not found");
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId)).thenReturn(null);
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto());
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(machine.isOnlineWeb()).thenReturn(true);

    // when
    siglusAdministrationsService.getFacility(facilityId);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenFacilityReportTypeStartDateInvalid() {
    // given
    String programCode = "Via";
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, true, false);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));
    SiglusReportTypeDto reportTypeDto = SiglusReportTypeDto.builder()
        .facilityId(facilityId)
        .programCode(programCode)
        .startDate(LocalDate.now())
        .build();
    SiglusFacilityDto mock = mockSiglusFacilityDto(false, null);
    mock.setReportTypes(Collections.singletonList(reportTypeDto));
    SiglusReportType reportType = SiglusReportType.from(reportTypeDto);
    reportType.setStartDate(reportTypeDto.getStartDate().plusDays(1L));
    when(siglusReportTypeRepository.findByFacilityId(facilityId)).thenReturn(Arrays.asList(reportType));
    when(siglusProcessingPeriodService.getPreviousPeriodStartDateSinceInitiate(programCode, facilityId))
        .thenReturn(LocalDate.now().plusDays(2L));

    // when
    siglusAdministrationsService.updateFacility(facilityId, mock);
  }

  @Test
  public void shouldEnableLocationManagementWhenFacilityExtensionIsNotNull() {
    // given
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, true, false);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));

    // when
    FacilitySearchResultDto searchResultDto = siglusAdministrationsService.updateFacility(facilityId,
        mockSiglusFacilityDto(true, null));

    // then
    assertTrue(searchResultDto.getEnableLocationManagement());
  }

  @Test
  public void shouldUnableLocationManagementWhenFacilityExtensionIsNull() {
    // given
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));
    when(androidHelper.isAndroid()).thenReturn(false);

    // when
    FacilitySearchResultDto searchResultDto = siglusAdministrationsService.updateFacility(facilityId,
        mockSiglusFacilityDto(false, null));

    // then
    assertFalse(searchResultDto.getEnableLocationManagement());
  }

  @Test
  public void shouldDeleteDraftsWhenEnableLocationManagementIfExtensionExisted() {
    // given
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, false, false);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));

    // when
    siglusAdministrationsService.updateFacility(facilityId, mockSiglusFacilityDto(true, null));

    // then
    verify(locationDraftRepository).deleteLocationRelatedDrafts(facilityId);
  }

  @Test
  public void shouldDeleteDraftsWhenEnableLocationManagementIfExtensionIsNull() {
    // given
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));

    // when
    siglusAdministrationsService.updateFacility(facilityId, mockSiglusFacilityDto(true, null));

    // then
    verify(locationDraftRepository).deleteLocationRelatedDrafts(facilityId);
  }

  @Test
  public void shouldDeleteDraftsWhenDisableLocationManagement() {
    // given
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, true, false);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));

    // when
    siglusAdministrationsService.updateFacility(facilityId, mockSiglusFacilityDto(false, null));

    // then
    verify(locationDraftRepository).deleteLocationRelatedDrafts(facilityId);
  }

  @Test
  public void shouldExportEmptyLocationManagementTemplate() {
    // given
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    when(locationManagementRepository.findByFacilityId(facilityId)).thenReturn(null);

    // when
    siglusAdministrationsService.exportLocationInfo(facilityId, httpServletResponse);

    // then
    verify(locationManagementRepository, times(1)).findByFacilityId(facilityId);
  }

  @Test
  public void shouldExportLocationManagementWhenHasRecords() {
    // given
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    when(locationManagementRepository.findByFacilityId(facilityId)).thenReturn(mockLocationManagement());

    // when
    siglusAdministrationsService.exportLocationInfo(facilityId, httpServletResponse);

    // then
    verify(locationManagementRepository, times(1)).findByFacilityId(facilityId);
  }

  @Test
  public void shouldThrowExceptionWhenUploadLocationManagement() throws IOException {
    // given
    exception.expect(ValidationMessageException.class);

    // when
    siglusAdministrationsService.uploadLocationInfo(facilityId, null);
  }

  @Test
  public void shouldThrowExceptionWhenUploadLocationManagementIsNotCsv() throws IOException {
    // given
    exception.expect(ValidationMessageException.class);
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");

    // when
    siglusAdministrationsService.uploadLocationInfo(facilityId, multipartFile);
  }

  @Test
  public void shouldDownloadLocationManagementCsvSuccessfully() throws IOException {
    // given
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
    InputStream inputStream = new ByteArrayInputStream(csvInput.getBytes(StandardCharsets.UTF_8));
    when(multipartFile.getInputStream()).thenReturn(inputStream);
    CSVParser parse = CSVParser.parse(csvInput, CSVFormat.EXCEL.withFirstRecordAsHeader());
    doNothing().when(csvValidator).validateCsvHeaders(parse);

    // when
    siglusAdministrationsService.uploadLocationInfo(facilityId, multipartFile);
  }

  @Test
  public void shouldCreateNewAndroidFacility() {
    // given
    SiglusFacilityDto siglusFacilityDto = mockSiglusFacilityDto(false, null);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.createFacility(siglusFacilityDto)).thenReturn(facilityDto);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId)).thenReturn(facilityDto);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(0);

    // when
    siglusAdministrationsService.createFacility(siglusFacilityDto);

    // then
    verify(facilityExtensionRepository, times(1))
        .save(mockFacilityExtension(facilityId, true, null, null));
  }

  @Test
  public void shouldAssignVirtualLocationsWhenEnableLocationManagementWhileInitialInventoryFinished() {
    // given
    StockCard stockCard = mockStockCard();
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId)).thenReturn(mockFacilityDto());
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(100);
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Lists.newArrayList(stockCard));
    when(calculatedStockOnHandRepository.findLatestStockOnHands(any(), any()))
        .thenReturn(Lists.newArrayList(mockCalculatedStockOnHand()));
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto());
    SiglusFacilityDto siglusFacilityDto = mockSiglusFacilityDto(true, "locationManagement");
    when(calculatedStocksOnHandLocationsRepository
        .findLatestLocationSohByStockCardIds(any()))
        .thenReturn(Lists.newArrayList(mockCalculatedStocksOnHandLocations()));
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(1);

    // when
    siglusAdministrationsService.updateFacility(facilityId, siglusFacilityDto);

    // then
    verify(stockCardLocationMovementLineItemRepository, times(0))
        .save(Lists.newArrayList());
  }

  @Test
  public void shouldAssignVirtualLocationsWhenDisableLocationManagementWhileInitialInventoryFinished() {
    // given
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId)).thenReturn(mockFacilityDto());
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(100);
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Lists.newArrayList(mockStockCard()));
    when(calculatedStockOnHandRepository.findLatestStockOnHands(Lists.newArrayList(stockCardId), ZonedDateTime.now()))
        .thenReturn(Lists.newArrayList(mockCalculatedStockOnHand()));
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto());
    SiglusFacilityDto siglusFacilityDto = mockSiglusFacilityDto(false, "locationManagement");
    when(calculatedStocksOnHandLocationsRepository.findLatestLocationSohByStockCardIds(
        Lists.newArrayList(stockCardId))).thenReturn(Lists.newArrayList(mockCalculatedStocksOnHandLocations()));

    // when
    siglusAdministrationsService.updateFacility(facilityId, siglusFacilityDto);

    // then
    verify(stockCardLocationMovementLineItemRepository, times(0))
        .save(Lists.newArrayList());
  }

  @Test
  public void shouldAssignVirtualLocationsWhenInitialInventoryNotStart() {
    // given
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(0);
    when(siglusFacilityReferenceDataService.findOneWithoutCache(facilityId)).thenReturn(mockFacilityDto());
    SiglusFacilityDto siglusFacilityDto = mockSiglusFacilityDto(true, "locationManagement");
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto());

    // when
    siglusAdministrationsService.updateFacility(facilityId, siglusFacilityDto);

    // then
    verify(stockCardLocationMovementLineItemRepository, times(0))
        .save(Lists.newArrayList());
  }

  @Test
  public void shouldThrowPermissionExceptionWhenTheFacilityIsNotAndroidFacility() {
    // given
    exception.expect(PermissionMessageException.class);
    when(facilityExtensionRepository.findByFacilityId(facilityId))
        .thenReturn(null);

    // when
    siglusAdministrationsService.upgradeAndroidFacilityToWeb(facilityId);
  }

  @Test
  public void shouldReturnWhenTheAndroidFacilityIsNewFacility() {
    // given
    when(facilityExtensionRepository.findByFacilityId(facilityId))
        .thenReturn(mockFacilityExtension(facilityId, true, false, false));
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(0);

    // when
    siglusAdministrationsService.upgradeAndroidFacilityToWeb(facilityId);

    // then
    verify(calculatedStockOnHandRepository, times(0))
        .deleteByFacilityIdAndOrderableIds(facilityId, new HashSet<>());
    verify(stockCardLineItemRepository, times(0)).deleteByStockCardIdIn(Lists.newArrayList());
    verify(stockCardRepository, times(0)).deleteByIdIn(Lists.newArrayList());
  }

  @Test
  public void shouldAllowAndroidFacilityUpgradeToWebFacility() {
    // given
    when(facilityExtensionRepository.findByFacilityId(facilityId))
        .thenReturn(mockFacilityExtension(facilityId, true, false, false));
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(1);
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Lists.newArrayList(mockStockCard()));
    when(orderableRepository.findLatestByIds(Lists.newArrayList(orderableId)))
        .thenReturn(Lists.newArrayList(mockOrderable()));

    // when
    siglusAdministrationsService.upgradeAndroidFacilityToWeb(facilityId);

    // then
    verify(calculatedStockOnHandRepository, times(1))
        .deleteByFacilityIdAndOrderableIds(facilityId, Collections.singleton(orderableId));
    verify(stockCardLineItemRepository, times(1))
        .deleteByStockCardIdIn(Lists.newArrayList(stockCardId));
    verify(stockCardRepository, times(1))
        .deleteByIdIn(Lists.newArrayList(stockCardId));
    verify(stockCardExtensionRepository, times(1))
        .deleteByStockCardIdIn(Lists.newArrayList(stockCardId));
  }

  @Test
  public void shouldReturnFacilityDeviceWhenIsAndroidCallByService() {
    //given
    AppInfo appInfo = mockAppInfo();
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, true, true, false);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    //when
    FacilityDeviceDto facilityDevice = siglusAdministrationsService.getFacilityDevice(facilityId);
    //then
    assertEquals(FacilityDeviceTypeEnum.ANDROID, facilityDevice.getDeviceType());
  }

  @Test
  public void shouldReturnFacilityDeviceWhenIsLocalMachineCallByService() {
    //given
    AppInfo appInfo = mockAppInfo();
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, true, true);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    //when
    FacilityDeviceDto facilityDevice = siglusAdministrationsService.getFacilityDevice(facilityId);
    //then
    assertEquals(FacilityDeviceTypeEnum.LOCAL_MACHINE, facilityDevice.getDeviceType());
  }

  @Test
  public void shouldReturnFacilityDeviceWhenIsWebCallByService() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, true, false);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    //when
    FacilityDeviceDto facilityDevice = siglusAdministrationsService.getFacilityDevice(facilityId);
    //then
    assertEquals(FacilityDeviceTypeEnum.WEB, facilityDevice.getDeviceType());
  }

  @Test
  public void shouldReturnFacilityDeviceWhenNoExtensionInfoCallByService() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = null;
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    //when
    FacilityDeviceDto facilityDevice = siglusAdministrationsService.getFacilityDevice(facilityId);
    //then
    assertEquals(FacilityDeviceTypeEnum.WEB, facilityDevice.getDeviceType());
  }

  @Test
  public void shoudlEraseDeviceInfoWhenAndroidFacilityCallByService() {
    //given
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    //when
    siglusAdministrationsService.eraseDeviceInfo(FacilityDeviceTypeEnum.ANDROID, facilityId);
    //then
    verify(appInfoRepository).deleteByFacilityCode(facilityDto.getCode());
  }

  @Test
  public void shoudlEraseDeviceInfoWhenLocalMachineFacilityCallByService() {
    //given
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    //when
    siglusAdministrationsService.eraseDeviceInfo(FacilityDeviceTypeEnum.LOCAL_MACHINE, facilityId);
    //then
    verify(agentInfoRepository).deleteByFacilityId(facilityId);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowWhenWebFacilityChangeToWeb() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, true, false);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToWeb(facilityId);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowWhenHasDeviceInfo() {
    //given
    AppInfo appInfo = new AppInfo();
    appInfo.setDeviceInfo("111");
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, true, true, false);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToWeb(facilityId);
  }


  @Test
  public void shouldChangeToWebWhenLocalMachineFacilityCallByService() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, true, true);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    when(facilityExtensionRepository.findByFacilityId(facilityId))
        .thenReturn(mockFacilityExtension(facilityId, true, false, false));
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(1);
    StockCard stockCard1 = mockStockCard();
    stockCard1.setLotId(UUID.randomUUID());
    StockCard stockCard2 = mockStockCard();
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Lists.newArrayList(stockCard1, stockCard2));
    when(orderableRepository.findLatestByIds(Lists.newArrayList(orderableId)))
        .thenReturn(Lists.newArrayList(mockOrderable()));
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToWeb(facilityId);
    facilityExtension.setIsAndroid(true);
  }

  @Test
  public void shouldChangeToWebWhenLocalMachineFacilityWithNoProductCallByService() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, true, true);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    when(facilityExtensionRepository.findByFacilityId(facilityId))
        .thenReturn(mockFacilityExtension(facilityId, true, false, false));
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(1);
    StockCard stockCard1 = mockStockCard();
    stockCard1.setOrderableId(null);
    stockCard1.setLotId(UUID.randomUUID());
    StockCard stockCard2 = mockStockCard();
    stockCard2.setOrderableId(null);
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Lists.newArrayList(stockCard1, stockCard2));
    when(orderableRepository.findLatestByIds(null))
        .thenReturn(Lists.newArrayList(mockOrderable()));
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToWeb(facilityId);
    facilityExtension.setIsAndroid(true);
  }

  @Test
  public void shouldChangeToWebWhenAndroidFacilityCallByService() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, true, true, false);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    when(facilityExtensionRepository.findByFacilityId(facilityId))
        .thenReturn(mockFacilityExtension(facilityId, true, false, false));
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(1);
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Lists.newArrayList(mockStockCard()));
    when(orderableRepository.findLatestByIds(Lists.newArrayList(orderableId)))
        .thenReturn(Lists.newArrayList(mockOrderable()));
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToWeb(facilityId);
    facilityExtension.setIsAndroid(true);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowWhenNotWebFacilityChangeToLocalMachine() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, true, true, false);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToLocalMachine(facilityId);
  }

  @Test
  public void shouldThrowWhenWithoutFacilityExtensionChangeToLocalMachine() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = null;
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToLocalMachine(facilityId);
  }

  @Test
  public void shouldThrowWhenWithFacilityExtensionChangeToLocalMachine() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, true, false);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToLocalMachine(facilityId);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenOldFacilityChangeToAndroid() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = null;
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(2);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToAndroid(facilityId);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenOtherFacilityChangeToAndroid() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, true, false, false);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(0);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToAndroid(facilityId);
  }

  @Test
  public void shouldChangeToAndroidWhenWithFacilityExtensionChangeToAndroid() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, false, false);
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(0);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToAndroid(facilityId);
    facilityExtension.setIsAndroid(true);
    facilityExtension.setFacilityCode(facilityDto.getCode());
    //then
    verify(facilityExtensionRepository).save(facilityExtension);
  }

  @Test
  public void shouldChangeToAndroidWhenWithoutFacilityExtensionChangeToAndroid() {
    //given
    AppInfo appInfo = null;
    FacilityExtension facilityExtension = null;
    FacilityDto facilityDto = mockFacilityDto();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(appInfoRepository.findByFacilityCode(facilityDto.getCode())).thenReturn(appInfo);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(0);
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());
    FacilityDeviceDto facilityDeviceDto = new FacilityDeviceDto();
    facilityDeviceDto.setDeviceType(FacilityDeviceTypeEnum.WEB);
    //when
    siglusAdministrationsService.changeToAndroid(facilityId);
    facilityExtension = mockFacilityExtension(facilityId, true, false, false);
    facilityExtension.setFacilityCode(facilityDto.getCode());
    //then
    verify(facilityExtensionRepository).save(facilityExtension);
  }

  private FacilitySearchParamDto mockFacilitySearchParamDto() {
    FacilitySearchParamDto facilitySearchParamDto = new FacilitySearchParamDto();
    facilitySearchParamDto.setName(Name);
    return facilitySearchParamDto;
  }

  private AppInfo mockAppInfo() {
    AppInfo appInfo = new AppInfo();
    appInfo.setId(facilityId);
    appInfo.setFacilityCode(facilityCode);
    return appInfo;
  }

  private Page<FacilityDto> mockFacilityDtoPage() {
    FacilityDto facilityInfo1 = new FacilityDto();
    facilityInfo1.setId(device1);
    facilityInfo1.setActive(true);

    FacilityDto facilityInfo2 = new FacilityDto();
    facilityInfo2.setId(device2);
    facilityInfo2.setActive(false);

    FacilityDto facilityInfo3 = new FacilityDto();
    facilityInfo3.setId(device3);
    facilityInfo3.setActive(true);

    content.add(facilityInfo1);
    content.add(facilityInfo2);
    content.add(facilityInfo3);
    return Pagination.getPage(content, pageable);
  }

  private FacilityExtension mockFacilityExtension(UUID facilityId, Boolean isAndroid, Boolean enableLocation,
      Boolean isLocalMachine) {
    FacilityExtension facilityExtension = new FacilityExtension();
    facilityExtension.setFacilityId(facilityId);
    facilityExtension.setIsAndroid(isAndroid);
    facilityExtension.setEnableLocationManagement(enableLocation);
    facilityExtension.setFacilityCode(facilityCode);
    facilityExtension.setIsLocalMachine(isLocalMachine);
    return facilityExtension;
  }

  private SiglusFacilityDto mockSiglusFacilityDto(boolean enableLocationManagement, String tab) {
    SiglusFacilityDto siglusFacilityDto = new SiglusFacilityDto();
    siglusFacilityDto.setId(facilityId);
    siglusFacilityDto.setIsAndroidDevice(true);
    siglusFacilityDto.setReportTypes(Collections.emptyList());
    siglusFacilityDto.setEnableLocationManagement(enableLocationManagement);
    siglusFacilityDto.setTab(tab);
    return siglusFacilityDto;
  }

  private List<FacilityLocations> mockLocationManagement() {
    FacilityLocations locationManagement = new FacilityLocations();
    locationManagement.setFacilityId(facilityId);
    locationManagement.setLocationCode(LOCATION_CODE);
    locationManagement.setArea(AREA);
    locationManagement.setRack(RACK);
    locationManagement.setZone(ZONE);
    locationManagement.setLevel(LEVEL);
    locationManagement.setBin(BIN);
    locationManagement.setBarcode(BARCODE);
    locationManagementList.add(locationManagement);
    return locationManagementList;
  }

  private FacilityDto mockFacilityDto() {
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(facilityId);
    facilityDto.setCode(facilityCode);
    return facilityDto;
  }

  private SiglusReportType mockReportType() {
    return SiglusReportType
        .builder()
        .facilityId(facilityId)
        .programCode(PROGRAM_CODE)
        .build();
  }

  private CalculatedStockOnHandByLocation mockCalculatedStocksOnHandLocations() {
    CalculatedStockOnHandByLocation calculatedStocksOnHandLocations = new CalculatedStockOnHandByLocation();
    calculatedStocksOnHandLocations.setStockCardId(stockCardId);
    calculatedStocksOnHandLocations.setLocationCode(LOCATION_CODE);
    calculatedStocksOnHandLocations.setArea(AREA);
    calculatedStocksOnHandLocations.setStockOnHand(100);
    return calculatedStocksOnHandLocations;
  }

  private StockCard mockStockCard() {
    StockCard stockCard = new StockCard();
    stockCard.setId(stockCardId);
    stockCard.setFacilityId(facilityId);
    stockCard.setOrderableId(orderableId);
    return stockCard;
  }

  private CalculatedStockOnHand mockCalculatedStockOnHand() {
    CalculatedStockOnHand calculatedStockOnHand = new CalculatedStockOnHand();
    calculatedStockOnHand.setStockOnHand(10);
    calculatedStockOnHand.setStockCard(mockStockCard());
    calculatedStockOnHand.setStockCardId(stockCardId);
    return calculatedStockOnHand;
  }

  private UserDto mockUserDto() {
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    userDto.setHomeFacilityId(facilityId);
    return userDto;
  }

  private Orderable mockOrderable() {
    return new Orderable(
        new Code(productCode),
        null, 1, 1, false, orderableId, 1L);
  }
}
