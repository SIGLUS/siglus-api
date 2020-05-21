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

package org.openlmis.requisition.service;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.APPROVED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.INITIATED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED_WITHOUT_ORDER;
import static org.openlmis.requisition.dto.TimelinessReportFacilityDto.DISTRICT_LEVEL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.JasperTemplate;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.GeographicLevelDto;
import org.openlmis.requisition.dto.GeographicZoneDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ReportingRateReportDto;
import org.openlmis.requisition.dto.RequisitionDto;
import org.openlmis.requisition.dto.RequisitionReportDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.custom.DefaultRequisitionSearchParams;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.GeographicZoneReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.web.ReportingRateReportDtoBuilder;
import org.openlmis.requisition.web.RequisitionReportDtoBuilder;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsMultiFormatView;

@SuppressWarnings({"PMD.TooManyMethods"})
@RunWith(MockitoJUnitRunner.class)
public class JasperReportsViewServiceTest {

  private static final String DATE_FORMAT = "dd/MM/yyyy";
  private static final String GROUPING_SEPARATOR = ",";
  private static final String GROUPING_SIZE = "3";
  private static final String PERIOD = "period";
  private static final String PROGRAM = "program";
  private static final String DISTRICT = "district";
  private static final String DEFAULT_LOCALE = "en";
  private static final String CURRENCY_LOCALE = "US";

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private GeographicZoneReferenceDataService geographicZoneReferenceDataService;

  @Mock
  private ReportingRateReportDtoBuilder reportingRateReportDtoBuilder;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private ByteArrayOutputStream byteArrayOutputStream;

  @Mock
  private ObjectInputStream objectInputStream;

  @Mock
  private JasperReportsMultiFormatView jasperReportsMultiFormatView;

  @Mock
  private JasperReport jasperReport;

  @Mock
  private RequisitionReportDtoBuilder requisitionReportDtoBuilder;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private RequisitionLineItem lineItem1;

  @Mock
  private RequisitionLineItem lineItem2;

  @InjectMocks
  private JasperReportsViewService service;

  @Spy
  private RequisitionTemplate requisitionTemplate = new RequisitionTemplateDataBuilder()
      .withAllColumns().build();

  private Requisition requisition;
  private ProgramDto program = DtoGenerator.of(ProgramDto.class);
  private ProcessingPeriodDto period = DtoGenerator.of(ProcessingPeriodDto.class);
  private GeographicZoneDto district = DtoGenerator.of(GeographicZoneDto.class);
  private RequisitionDto requisitionDto = DtoGenerator.of(RequisitionDto.class);
  private FacilityDto facility = DtoGenerator.of(FacilityDto.class);
  private SupervisoryNodeDto supervisoryNode = DtoGenerator.of(SupervisoryNodeDto.class);
  private Locale locale = new Locale(DEFAULT_LOCALE, CURRENCY_LOCALE);

  private Map<String, Object> reportParams = new HashMap<>();

  private JasperTemplate jasperTemplate;

  @Before
  public void setUp() throws Exception {
    generateRequisition();
    service = spy(new JasperReportsViewService());

    ReflectionTestUtils.setField(service, "dateFormat", DATE_FORMAT);
    ReflectionTestUtils.setField(service, "groupingSeparator", GROUPING_SEPARATOR);
    ReflectionTestUtils.setField(service, "groupingSize", GROUPING_SIZE);
    ReflectionTestUtils.setField(service, "defaultLocale", DEFAULT_LOCALE);
    ReflectionTestUtils.setField(service, "currencyLocale", CURRENCY_LOCALE);

    jasperTemplate = mock(JasperTemplate.class);
    when(jasperTemplate.getName()).thenReturn("report1.jrxml");
    byte[] reportByteData = new byte[1];
    when(jasperTemplate.getData()).thenReturn(reportByteData);

    ObjectOutputStream objectOutputStream = createObjectOutputStream();

    doReturn(objectInputStream).when(service).createObjectInputStream(jasperTemplate);
    doReturn(byteArrayOutputStream).when(service).createByteArrayOutputStream();
    doReturn(objectOutputStream).when(service).createObjectOutputStream(byteArrayOutputStream);
    doReturn(jasperReportsMultiFormatView).when(service).createJasperMultiFormatView();
    doReturn(jasperReport).when(service).readReportData(objectInputStream);
    doReturn(new byte[0]).when(byteArrayOutputStream).toByteArray();

    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void shouldSetViewParamsForTimelinessReport() {
    //given
    UUID districtId = UUID.randomUUID();
    reportParams.put(PROGRAM, program.getId().toString());
    reportParams.put(PERIOD, period.getId().toString());
    reportParams.put(DISTRICT, districtId.toString());
    when(programReferenceDataService.findOne(program.getId())).thenReturn(program);
    when(periodReferenceDataService.findOne(period.getId())).thenReturn(period);
    when(geographicZoneReferenceDataService.findOne(districtId)).thenReturn(district);

    // when
    ModelAndView view = service.getTimelinessJasperReportView(
        new JasperReportsMultiFormatView(), reportParams);
    Map<String, Object> outputParams = view.getModel();
    List<FacilityDto> facilities = extractFacilitiesFromOutputParams(outputParams);

    // then
    Assert.assertEquals(program, outputParams.get(PROGRAM));
    Assert.assertEquals(period, outputParams.get(PERIOD));
    Assert.assertEquals(district, outputParams.get(DISTRICT));
    Assert.assertEquals(facilities, Collections.emptyList());
  }

  @Test
  public void shouldGetTimelinessReportViewWithActiveFacilitiesMissingRnR() {
    // given
    reportParams.put(PROGRAM, program.getId().toString());
    reportParams.put(PERIOD, period.getId().toString());

    when(programReferenceDataService.findOne(program.getId())).thenReturn(program);
    when(periodReferenceDataService.findOne(period.getId())).thenReturn(period);

    List<FacilityDto> facilitiesToReturn = new ArrayList<>();

    // active facilities missing RnR
    FacilityDto facility = mockFacility(true, true, UUID.randomUUID(), "Test", "Test");
    FacilityDto anotherFacility = mockFacility(true, true,
        UUID.randomUUID(), "zone", "facility");
    facilitiesToReturn.add(facility);
    facilitiesToReturn.add(anotherFacility);

    // "on time" active facility
    facilitiesToReturn.add(mockFacility(true, false));

    // inactive facilities
    facilitiesToReturn.add(mockFacility(false, false));
    facilitiesToReturn.add(mockFacility(false, true));

    when(facilityReferenceDataService.findAll()).thenReturn(facilitiesToReturn);

    // when
    ModelAndView view = service.getTimelinessJasperReportView(
        new JasperReportsMultiFormatView(), reportParams);
    List<FacilityDto> facilities = extractFacilitiesFromOutputParams(view.getModel());

    // then
    Assert.assertEquals(2, facilities.size());
    List<UUID> facilityIds = facilities.stream()
        .map(FacilityDto::getId).collect(Collectors.toList());
    Assert.assertTrue(facilityIds.contains(facility.getId()));
    Assert.assertTrue(facilityIds.contains(anotherFacility.getId()));
  }

  @Test
  public void shouldGetTimelinessReportViewWithFacilitiesFromSpecifiedDistrict() {
    //given
    UUID districtId = UUID.randomUUID();
    reportParams.put(DISTRICT, districtId.toString());
    reportParams.put(PROGRAM, program.getId().toString());
    reportParams.put(PERIOD, period.getId().toString());

    when(programReferenceDataService.findOne(program.getId())).thenReturn(program);
    when(periodReferenceDataService.findOne(period.getId())).thenReturn(period);

    // active facilities missing RnR
    MinimalFacilityDto facility = mockBasicFacility(true, true, districtId, "parent-zone", "f1");
    MinimalFacilityDto childFacility =
        mockBasicFacility(true, true, UUID.randomUUID(), "child-zone", "f2");

    GeographicLevelDto childLevel = mock(GeographicLevelDto.class);
    when(childLevel.getLevelNumber()).thenReturn(DISTRICT_LEVEL + 1);

    GeographicZoneDto childZone = childFacility.getGeographicZone();
    childZone.setParent(facility.getGeographicZone());
    childZone.setLevel(childLevel);

    // facility missing RnR from another district
    mockFacility(true, true);

    when(facilityReferenceDataService.search(any(), any(), eq(districtId), eq(true)))
        .thenReturn(Arrays.asList(facility, childFacility));

    // when
    ModelAndView view = service.getTimelinessJasperReportView(
        new JasperReportsMultiFormatView(), reportParams);
    List<FacilityDto> facilities = extractFacilitiesFromOutputParams(view.getModel());

    // then
    Assert.assertEquals(2, facilities.size());
    List<UUID> facilityIds = facilities.stream()
        .map(FacilityDto::getId).collect(Collectors.toList());
    Assert.assertTrue(facilityIds.contains(facility.getId()));
    Assert.assertTrue(facilityIds.contains(childFacility.getId()));
  }

  @Test
  public void shouldGetTimelinessReportViewWithFacilitiesFromAllZonesIfDistrictNotSpecified() {
    //given
    reportParams.put(PROGRAM, program.getId().toString());
    reportParams.put(PERIOD, period.getId().toString());

    when(programReferenceDataService.findOne(program.getId())).thenReturn(program);
    when(periodReferenceDataService.findOne(period.getId())).thenReturn(period);

    UUID zone1Id = UUID.randomUUID();
    UUID zone2Id = UUID.randomUUID();

    List<FacilityDto> facilitiesToReturn = Arrays.asList(
        // active facilities missing RnR from different zones
        mockFacility(true, true, zone1Id, "district A", "f1"),
        mockFacility(true, true, zone1Id, "district A", "f2"),
        mockFacility(true, true, zone2Id, "district B", "f3"),
        mockFacility(true, true, zone2Id, "district B", "f4")
    );

    when(facilityReferenceDataService.findAll()).thenReturn(facilitiesToReturn);

    // when
    ModelAndView view = service.getTimelinessJasperReportView(
        new JasperReportsMultiFormatView(), reportParams);
    List<FacilityDto> facilities = extractFacilitiesFromOutputParams(view.getModel());

    // then
    Assert.assertEquals(4, facilities.size());
    List<UUID> facilityIds = facilities.stream()
        .map(FacilityDto::getId).collect(Collectors.toList());
    for (MinimalFacilityDto facility : facilitiesToReturn) {
      Assert.assertTrue(facilityIds.contains(facility.getId()));
    }
  }

  @Test
  public void shouldSortFacilitiesReturnedWithTimelinessReportView() {
    //given
    reportParams.put(PROGRAM, program.getId().toString());
    reportParams.put(PERIOD, period.getId().toString());

    when(programReferenceDataService.findOne(program.getId())).thenReturn(program);
    when(periodReferenceDataService.findOne(period.getId())).thenReturn(period);

    UUID zone1Id = UUID.randomUUID();
    UUID zone2Id = UUID.randomUUID();

    FacilityDto facility1A = mockFacility(true, true, zone1Id, "zone1", "facilityA");
    FacilityDto facility1B = mockFacility(true, true, zone1Id, "zone1", "facilityB");
    FacilityDto facility2A = mockFacility(true, true, zone2Id, "zone2", "facilityA");
    FacilityDto facility2B = mockFacility(true, true, zone2Id, "zone2", "facilityB");

    when(facilityReferenceDataService.findAll()).thenReturn(Arrays.asList(
        facility2B, facility2A, facility1A, facility1B));

    // when
    ModelAndView view = service.getTimelinessJasperReportView(
        new JasperReportsMultiFormatView(), reportParams);
    List<FacilityDto> facilities = extractFacilitiesFromOutputParams(view.getModel());

    // then
    Assert.assertEquals(4, facilities.size());
    Assert.assertEquals(facility1A.getId(), facilities.get(0).getId());
    Assert.assertEquals(facility1B.getId(), facilities.get(1).getId());
    Assert.assertEquals(facility2A.getId(), facilities.get(2).getId());
    Assert.assertEquals(facility2B.getId(), facilities.get(3).getId());
  }

  @Test
  public void shouldSetParamsForReportingRateReport() throws Exception {
    UUID districtId = UUID.randomUUID();
    reportParams.put("Program", program.getId().toString());
    reportParams.put("Period", period.getId().toString());
    reportParams.put("GeographicZone", districtId.toString());
    reportParams.put("DueDays", "10");
    when(geographicZoneReferenceDataService.findOne(districtId)).thenReturn(district);

    ReportingRateReportDto reportingRateReportDto = DtoGenerator.of(ReportingRateReportDto.class);
    when(reportingRateReportDtoBuilder.build(program, period, district, 10))
        .thenReturn(reportingRateReportDto);

    ServletContext servletContext = new MockServletContext("");
    HttpServletRequest httpServletRequest = new MockHttpServletRequest(servletContext);

    service.getReportingRateJasperReportsView(jasperTemplate, httpServletRequest, reportParams);

    Assert.assertEquals(DATE_FORMAT, reportParams.get("dateFormat"));
    Assert.assertEquals(createDecimalFormat(), reportParams.get("decimalFormat"));
  }

  @Test
  public void shouldSetParamsForRequisitionReport() throws Exception {
    doReturn(locale).when(service).getLocaleFromService();
    when(requisitionRepository.findOne(requisitionDto.getId())).thenReturn(requisition);
    reportParams.put("Requisition", requisition.getId());

    RequisitionReportDto requisitionReportDto = DtoGenerator.of(RequisitionReportDto.class);
    when(requisitionReportDtoBuilder.build(requisition)).thenReturn(requisitionReportDto);

    ServletContext servletContext = new MockServletContext("");
    HttpServletRequest httpServletRequest = new MockHttpServletRequest(servletContext);

    ModelAndView view = service.getRequisitionJasperReportView(requisition, httpServletRequest);
    Map<String, Object> outputParams = view.getModel();

    Assert.assertEquals(DATE_FORMAT, outputParams.get("dateFormat"));
    Assert.assertEquals(createDecimalFormat(), outputParams.get("decimalFormat"));
    Assert.assertEquals(NumberFormat.getCurrencyInstance(locale),
        outputParams.get("currencyDecimalFormat"));
  }

  private List<FacilityDto> extractFacilitiesFromOutputParams(Map<String, Object> outputParams) {
    JRBeanCollectionDataSource datasource =
        (JRBeanCollectionDataSource) outputParams.get("datasource");
    return (List<FacilityDto>) datasource.getData();
  }

  private FacilityDto mockFacility(boolean isActive, boolean isMissingRnR) {
    return mockFacility(isActive, isMissingRnR, UUID.randomUUID(), "test", "test");
  }

  private FacilityDto mockFacility(boolean isActive, boolean isMissingRnR, UUID districtId,
                                   String districtName, String facilityName) {
    FacilityDto facility = new FacilityDto();

    UUID facilityId = UUID.randomUUID();
    facility.setId(facilityId);
    facility.setActive(isActive);
    facility.setName(facilityName);

    facility.setGeographicZone(
        mockGeographicZone(districtId, districtName, facilityId, isMissingRnR));

    return facility;
  }

  private MinimalFacilityDto mockBasicFacility(boolean isActive, boolean isMissingRnR,
                                               UUID districtId, String districtName,
                                               String facilityName) {
    MinimalFacilityDto facility = new MinimalFacilityDto();

    UUID facilityId = UUID.randomUUID();
    facility.setId(facilityId);
    facility.setActive(isActive);
    facility.setName(facilityName);

    facility.setGeographicZone(
        mockGeographicZone(districtId, districtName, facilityId, isMissingRnR));

    return facility;
  }

  private GeographicZoneDto mockGeographicZone(UUID districtId, String districtName,
                                               UUID facilityId, boolean isMissingRnR) {
    GeographicZoneDto geographicZoneDto = mock(GeographicZoneDto.class);
    when(geographicZoneDto.getId()).thenReturn(districtId);
    when(geographicZoneDto.getName()).thenReturn(districtName);

    GeographicLevelDto geographicLevelDto = mock(GeographicLevelDto.class);
    when(geographicLevelDto.getLevelNumber()).thenReturn(DISTRICT_LEVEL);
    when(geographicZoneDto.getLevel()).thenReturn(geographicLevelDto);
    when(geographicZoneReferenceDataService.findOne(districtId)).thenReturn(geographicZoneDto);

    Requisition mockRequisition = mock(Requisition.class);
    Page<Requisition> requisitionPage = mock(Page.class);
    when(requisitionPage.getContent()).thenReturn(
        (isMissingRnR) ? Collections.emptyList() : singletonList(mockRequisition));

    RequisitionSearchParams params = new DefaultRequisitionSearchParams(
        facilityId, program.getId(), period.getId(), null, null, null, null,
        null, null, EnumSet.of(APPROVED, RELEASED, RELEASED_WITHOUT_ORDER));

    when(requisitionService.searchRequisitions(eq(params), any()))
        .thenReturn(requisitionPage);

    return geographicZoneDto;
  }

  private DecimalFormat createDecimalFormat() {
    DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
    decimalFormatSymbols.setGroupingSeparator(GROUPING_SEPARATOR.charAt(0));
    DecimalFormat decimalFormat = new DecimalFormat("", decimalFormatSymbols);
    decimalFormat.setGroupingSize(Integer.valueOf(GROUPING_SIZE));
    return decimalFormat;
  }

  private Requisition generateRequisition() {
    requisition = new Requisition(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        INITIATED, false);
    requisition.setId(UUID.randomUUID());
    requisition.setCreatedDate(ZonedDateTime.now());
    requisition.setSupplyingFacilityId(facility.getId());
    List<RequisitionLineItem> requisitionLineItems = new ArrayList<>();
    requisitionLineItems.add(lineItem1);
    requisitionLineItems.add(lineItem2);
    requisition.setRequisitionLineItems(requisitionLineItems);
    requisition.setTemplate(requisitionTemplate);
    requisition.setFacilityId(facility.getId());
    requisition.setProgramId(program.getId());
    requisition.setSupervisoryNodeId(supervisoryNode.getId());
    requisition.setStatus(AUTHORIZED);
    return requisition;
  }

  // We use in the service writeObject method which has the final modifier
  // and we can't mock the method.
  private ObjectOutputStream createObjectOutputStream() throws IOException {
    return new ObjectOutputStream() {

      @Override
      protected final void writeObjectOverride(Object obj) {
        // nothing to do here
      }

      @Override
      public void flush() {
        // nothing to do here
      }

      @Override
      public void close() {
        // nothing to do here
      }
    };
  }
}
