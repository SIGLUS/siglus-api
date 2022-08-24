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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.openlmis.requisition.utils.Pagination;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.SiglusReportType;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilitySearchParamDto;
import org.siglus.siglusapi.dto.FacilitySearchResultDto;
import org.siglus.siglusapi.dto.SiglusFacilityDto;
import org.siglus.siglusapi.dto.SiglusReportTypeDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
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
public class SiglusAdminstrationServiceTest {

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
  private SiglusAuthenticationHelper authenticationHelper;
  @Rule
  public ExpectedException exception = ExpectedException.none();
  private static final UUID facilityId = UUID.randomUUID();
  private static final UUID device1 = UUID.randomUUID();
  private static final UUID device2 = UUID.randomUUID();
  private static final UUID device3 = UUID.randomUUID();
  private static final UUID stockCardId = UUID.randomUUID();
  private static final UUID userId = UUID.randomUUID();
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
        mockFacilityExtension(device1, true, false));
    when(facilityExtensionRepository.findByFacilityId(device2)).thenReturn(
        mockFacilityExtension(device2, false, false));
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
    when(siglusFacilityReferenceDataService.findOneFacility(facilityId))
            .thenReturn(mockFacilityDtoPage().getContent().get(0));

    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);

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
    when(siglusFacilityReferenceDataService.findOneFacility(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));

    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);

    // when
    FacilitySearchResultDto facility = siglusAdministrationsService.getFacility(facilityId);

    // then
    assertFalse(facility.getEnableLocationManagement());
  }

  @Test
  public void shouldGetFacilityInfoWhenFacilityExistsAndFacilityExtensionIsNotNull() {
    // given
    when(siglusFacilityReferenceDataService.findOneFacility(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));

    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(
        mockFacilityExtension(facilityId, false, false));

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
    when(siglusFacilityReferenceDataService.findOneFacility(facilityId)).thenReturn(null);

    // when
    siglusAdministrationsService.getFacility(facilityId);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenFacilityReportTypeStartDateInvalid() {
    // given
    String programCode = "Via";
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, true);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(siglusFacilityReferenceDataService.findOneFacility(facilityId))
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
    FacilityExtension facilityExtension = mockFacilityExtension(facilityId, false, true);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);
    when(siglusFacilityReferenceDataService.findOneFacility(facilityId))
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
    when(siglusFacilityReferenceDataService.findOneFacility(facilityId))
        .thenReturn(mockFacilityDtoPage().getContent().get(0));
    when(androidHelper.isAndroid()).thenReturn(false);

    // when
    FacilitySearchResultDto searchResultDto = siglusAdministrationsService.updateFacility(facilityId,
        mockSiglusFacilityDto(false, null));

    // then
    assertFalse(searchResultDto.getEnableLocationManagement());
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
    when(siglusFacilityReferenceDataService.findOneFacility(facilityId)).thenReturn(facilityDto);
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(0);

    // when
    siglusAdministrationsService.createFacility(siglusFacilityDto);

    // then
    verify(facilityExtensionRepository, times(1))
        .save(mockFacilityExtension(facilityId, true, null));
  }

  @Test
  public void shouldAssignVirtualLocationsWhenEnableLocationManagementWhileInitialInventoryFinished() {
    // given
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(siglusFacilityReferenceDataService.findOneFacility(facilityId)).thenReturn(mockFacilityDto());
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(100);
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Lists.newArrayList(mockStockCard()));
    when(calculatedStockOnHandRepository.findPreviousStockOnHands(Lists.newArrayList(stockCardId), LocalDate.now()))
        .thenReturn(Lists.newArrayList(mockCalculatedStockOnHand()));
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto());
    SiglusFacilityDto siglusFacilityDto = mockSiglusFacilityDto(true, "locationManagement");

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
    when(siglusFacilityReferenceDataService.findOneFacility(facilityId)).thenReturn(mockFacilityDto());
    when(stockCardRepository.countByFacilityId(facilityId)).thenReturn(100);
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Lists.newArrayList(mockStockCard()));
    when(calculatedStockOnHandRepository.findPreviousStockOnHands(Lists.newArrayList(stockCardId), LocalDate.now()))
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
    when(siglusFacilityReferenceDataService.findOneFacility(facilityId)).thenReturn(mockFacilityDto());
    SiglusFacilityDto siglusFacilityDto = mockSiglusFacilityDto(true, "locationManagement");
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto());

    // when
    siglusAdministrationsService.updateFacility(facilityId, siglusFacilityDto);

    // then
    verify(stockCardLocationMovementLineItemRepository, times(0))
        .save(Lists.newArrayList());
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

  private FacilityExtension mockFacilityExtension(UUID facilityId, Boolean isAndroid, Boolean enableLocation) {
    FacilityExtension facilityExtension = new FacilityExtension();
    facilityExtension.setFacilityId(facilityId);
    facilityExtension.setIsAndroid(isAndroid);
    facilityExtension.setEnableLocationManagement(enableLocation);
    facilityExtension.setFacilityCode(facilityCode);
    return facilityExtension;
  }

  private SiglusFacilityDto mockSiglusFacilityDto(boolean enableLocationManagement, String tab) {
    SiglusFacilityDto siglusFacilityDto = new SiglusFacilityDto();
    siglusFacilityDto.setId(facilityId);
    siglusFacilityDto.setEnableLocationManagement(true);
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
    return stockCard;
  }

  private CalculatedStockOnHand mockCalculatedStockOnHand() {
    CalculatedStockOnHand calculatedStockOnHand = new CalculatedStockOnHand();
    calculatedStockOnHand.setStockOnHand(100);
    calculatedStockOnHand.setStockCard(mockStockCard());
    return calculatedStockOnHand;
  }

  private UserDto mockUserDto() {
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    return userDto;
  }
}
