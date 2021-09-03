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

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.javers.common.collections.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.testutils.StockCardDtoDataBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsPdfView;

@RunWith(MockitoJUnitRunner.class)
public class SiglusJasperReportServiceTest {

  private static final String DATE_FORMAT = "dd/MM/yyyy";
  private static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss";
  private static final String GROUPING_SEPARATOR = ",";
  private static final String GROUPING_SIZE = "3";

  @InjectMocks
  private SiglusJasperReportService siglusJasperReportService;

  @Mock
  private SiglusStockCardService siglusStockCardService;

  @Mock
  private JasperReportsPdfView jasperReportsPdfView;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private StockCardSummariesService stockCardSummariesService;

  @Mock
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Before
  public void setUp() {
    siglusJasperReportService = spy(new SiglusJasperReportService());
    ReflectionTestUtils.setField(siglusJasperReportService, "dateFormat", DATE_FORMAT);
    ReflectionTestUtils.setField(siglusJasperReportService, "dateTimeFormat", DATE_TIME_FORMAT);
    ReflectionTestUtils.setField(siglusJasperReportService,
        "groupingSeparator", GROUPING_SEPARATOR);
    ReflectionTestUtils.setField(siglusJasperReportService, "groupingSize", GROUPING_SIZE);
    MockitoAnnotations.initMocks(this);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void shouldThrowResourceNotFoundExceptionWhenStockCardByLotNotExists() {
    UUID stockCardId = UUID.randomUUID();
    when(siglusStockCardService.findStockCardById(stockCardId)).thenReturn(null);

    siglusJasperReportService.getStockCardByLotReportView(stockCardId);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void shouldThrowResourceNotFoundExceptionWhenStockCardByOrderableNotExists() {
    UUID stockCardId = UUID.randomUUID();
    when(siglusStockCardService.findStockCardByOrderable(stockCardId)).thenReturn(null);

    siglusJasperReportService.getStockCardByOrderableReportView(stockCardId);
  }

  @Test
  public void shouldGenerateReportWithProperParamsIfStockCardExists() {
    StockCardDto stockCard = StockCardDtoDataBuilder.createStockCardDto();
    when(siglusStockCardService.findStockCardById(stockCard.getId())).thenReturn(stockCard);

    doReturn(jasperReportsPdfView).when(siglusJasperReportService).createJasperReportsPdfView();

    ModelAndView report = siglusJasperReportService.getStockCardByLotReportView(stockCard.getId());
    Map<String, Object> outputParams = report.getModel();

    assertEquals(singletonList(stockCard), outputParams.get("datasource"));
    assertEquals(stockCard.hasLot(), outputParams.get("hasLot"));
    assertEquals(DATE_FORMAT, outputParams.get("dateFormat"));
    assertEquals(createDecimalFormat(), outputParams.get("decimalFormat"));
  }

  @Test
  public void shouldGenerateReportWithProperParamsIfStockCardSummaryExists() {
    StockCardDto stockCard = StockCardDtoDataBuilder.createStockCardDto();
    UUID programId = ALL_PRODUCTS_PROGRAM_ID;
    UUID facilityId = UUID.randomUUID();
    Set<UUID> programIds = Sets.asSet(UUID.randomUUID(), UUID.randomUUID());

    doReturn(jasperReportsPdfView).when(siglusJasperReportService).createJasperReportsPdfView();

    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto());
    when(siglusStockCardSummariesService.getProgramIds(any(), any(), any(), any()))
        .thenReturn(programIds);
    when(stockCardSummariesService.findStockCards(programIds, facilityId))
        .thenReturn(singletonList(stockCard));

    ModelAndView report = siglusJasperReportService
        .getStockCardSummariesReportView(programId, facilityId);
    Map<String, Object> outputParams = report.getModel();

    assertEquals(singletonList(stockCard), outputParams.get("stockCardSummaries"));
    assertEquals(stockCard.getProgram(), outputParams.get("program"));
    assertEquals(stockCard.getFacility(), outputParams.get("facility"));
    assertEquals(DATE_TIME_FORMAT, outputParams.get("dateTimeFormat"));
    assertEquals(DATE_FORMAT, outputParams.get("dateFormat"));
    assertEquals(false, outputParams.get("showLot"));
    assertEquals(createDecimalFormat(), outputParams.get("decimalFormat"));
  }

  private DecimalFormat createDecimalFormat() {
    DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
    decimalFormatSymbols.setGroupingSeparator(GROUPING_SEPARATOR.charAt(0));
    DecimalFormat decimalFormat = new DecimalFormat("", decimalFormatSymbols);
    decimalFormat.setGroupingSize(Integer.valueOf(GROUPING_SIZE));
    return decimalFormat;
  }

  private UserDto mockUserDto() {
    UserDto userDto = new UserDto();
    userDto.setId(UUID.randomUUID());
    return userDto;
  }

}
