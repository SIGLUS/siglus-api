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

package org.siglus.siglusapi.web;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.siglus.common.domain.referencedata.Facility;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.LotMovement;
import org.siglus.siglusapi.dto.android.MovementDetail;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.ProductLotStock;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.SiglusFcIntegrationService;
import org.siglus.siglusapi.service.android.mapper.ProductMovementMapper;
import org.siglus.siglusapi.service.android.mapper.ProductMovementMapperImpl;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.mapper.LotOnHandMapper;
import org.siglus.siglusapi.service.mapper.LotOnHandMapperImpl;
import org.siglus.siglusapi.web.android.FileBasedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {MappingJackson2HttpMessageConverter.class, PageableHandlerMethodArgumentResolver.class,
    ObjectMapper.class, ProductMovementMapperImpl.class, LotOnHandMapperImpl.class})
public class SiglusFcIntegrationControllerMvcTest extends FileBasedTest {

  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private MappingJackson2HttpMessageConverter jackson2HttpMessageConverter;
  @Autowired
  private PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver;
  @Autowired
  private ProductMovementMapper productMovementMapper;
  @Autowired
  private LotOnHandMapper lotOnHandMapper;

  @InjectMocks
  private SiglusFcIntegrationService service;

  @Mock
  private SiglusFacilityTypeReferenceDataService facilityTypeDataService;
  @Mock
  private SiglusFacilityRepository facilityRepo;
  @Mock
  private StockManagementRepository stockManagementRepository;

  private final UUID facility1Id = UUID.randomUUID();
  private final UUID facility2Id = UUID.randomUUID();

  @Before
  public void setup() throws JsonProcessingException {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    ReflectionTestUtils.setField(service, "productMovementMapper", productMovementMapper);
    ReflectionTestUtils.setField(service, "lotOnHandMapper", lotOnHandMapper);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    objectMapper.registerModule(new JavaTimeModule());
    jackson2HttpMessageConverter.setObjectMapper(objectMapper);
    SiglusFcIntegrationController controller = new SiglusFcIntegrationController(service);
    this.mockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setMessageConverters(jackson2HttpMessageConverter)
        .setCustomArgumentResolvers(pageableHandlerMethodArgumentResolver)
        .build();
    when(facilityTypeDataService.getPage(any())).thenReturn(new PageImpl<>(emptyList()));
    Facility facility1 = new Facility();
    facility1.setId(facility1Id);
    facility1.setCode("F1");
    facility1.setName("Facility 1");
    Facility facility2 = new Facility();
    facility2.setId(facility2Id);
    facility2.setCode("F2");
    facility2.setName("Facility 2");
    when(facilityRepo.findAllExcept(any(), any())).thenReturn(new PageImpl<>(asList(facility1, facility2)));
    when(stockManagementRepository.getStockOnHand(eq(facility1Id), any())).thenReturn(mockStocksOnHand1());
    when(stockManagementRepository.getStockOnHand(eq(facility2Id), any())).thenReturn(mockStocksOnHand2());
    PeriodOfProductMovements period1 = new PeriodOfProductMovements(mockProductMovements1(), mockStocksOnHand1());
    when(stockManagementRepository.getAllProductMovementsForSync(eq(facility1Id), any())).thenReturn(period1);
    PeriodOfProductMovements period2 = new PeriodOfProductMovements(emptyList(), mockStocksOnHand2());
    when(stockManagementRepository.getAllProductMovementsForSync(eq(facility2Id), any())).thenReturn(period2);
  }

  @Test
  public void shouldReturnAllWhenGetStockMovements() throws Exception {
    // given
    RequestBuilder request = get("/api/siglusapi/integration/stockMovements?date=20210101")
        .contentType(MediaType.APPLICATION_JSON)
        .characterEncoding("utf-8");

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    String target = readFromFile("stockMovements.json");
    String response = resultActions.andReturn().getResponse().getContentAsString();
    boolean equals = objectMapper.readValue(target, Map.class).equals(objectMapper.readValue(response, Map.class));
    assertTrue(equals);
  }

  @Test
  public void shouldReturnAllWhenGetSohGivenDate() throws Exception {
    // given
    RequestBuilder request = get("/api/siglusapi/integration/stockOnHand?date=20210101")
        .contentType(MediaType.APPLICATION_JSON)
        .characterEncoding("utf-8");

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    resultActions.andExpect(status().isOk())
        .andExpect(jsonPath("content[0].facilityCode").value("F1"))
        .andExpect(jsonPath("content[0].facilityName").value("Facility 1"))
        .andExpect(jsonPath("content[0].products[0].productCode").value("test"))
        .andExpect(jsonPath("content[0].products[0].productName").value("Test"))
        .andExpect(jsonPath("content[0].products[0].stockOnHand").value(27))
        .andExpect(jsonPath("content[0].products[0].dateOfStock").value("2021-10-01"))
        .andExpect(jsonPath("content[0].products[0].lots[0].lotCode").value("lot1"))
        .andExpect(jsonPath("content[0].products[0].lots[0].expirationDate").value("2023-12-31"))
        .andExpect(jsonPath("content[0].products[0].lots[0].stockOnHand").value(10))
        .andExpect(jsonPath("content[0].products[0].lots[0].dateOfStock").value("2021-10-01"))
        .andExpect(jsonPath("content[0].products[0].lots[1].lotCode").value("lot2"))
        .andExpect(jsonPath("content[0].products[0].lots[1].expirationDate").value("2023-12-31"))
        .andExpect(jsonPath("content[0].products[0].lots[1].stockOnHand").value(9))
        .andExpect(jsonPath("content[0].products[0].lots[1].dateOfStock").value("2021-09-15"))
        .andExpect(jsonPath("content[0].products[0].lots[2].lotCode").value("lot3"))
        .andExpect(jsonPath("content[0].products[0].lots[2].expirationDate").value("2023-12-31"))
        .andExpect(jsonPath("content[0].products[0].lots[2].stockOnHand").value(8))
        .andExpect(jsonPath("content[0].products[0].lots[2].dateOfStock").value("2021-08-31"))
        .andExpect(jsonPath("content[0].products[1].productCode").value("26A01"))
        .andExpect(jsonPath("content[0].products[1].productName").value("Some kit"))
        .andExpect(jsonPath("content[0].products[1].stockOnHand").value(10))
        .andExpect(jsonPath("content[0].products[1].dateOfStock").value("2021-10-31"))
        .andExpect(jsonPath("content[0].products[1].lots.length()").value(0))
        .andExpect(jsonPath("content[1].facilityCode").value("F2"))
        .andExpect(jsonPath("content[1].facilityName").value("Facility 2"))
        .andExpect(jsonPath("content[1].products.length()").value(0));
  }

  private StocksOnHand mockStocksOnHand1() {
    ProductLotStock lot1 = ProductLotStock.builder()
        .code(ProductLotCode.of("test", "lot1"))
        .productName("Test")
        .stockQuantity(10)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 10, 1), Instant.now()))
        .expirationDate(java.sql.Date.valueOf(LocalDate.of(2023, 12, 31)))
        .build();
    ProductLotStock lot2 = ProductLotStock.builder()
        .code(ProductLotCode.of("test", "lot2"))
        .productName("Test")
        .stockQuantity(9)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 9, 15), Instant.now()))
        .expirationDate(java.sql.Date.valueOf(LocalDate.of(2023, 12, 31)))
        .build();
    ProductLotStock lot3 = ProductLotStock.builder()
        .code(ProductLotCode.of("test", "lot3"))
        .productName("Test")
        .stockQuantity(8)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 8, 31), Instant.now()))
        .expirationDate(java.sql.Date.valueOf(LocalDate.of(2023, 12, 31)))
        .build();
    ProductLotStock kitLot = ProductLotStock.builder()
        .code(ProductLotCode.of("26A01", null))
        .productName("Some kit")
        .stockQuantity(10)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 10, 31), Instant.now()))
        .build();
    return new StocksOnHand(asList(lot1, lot2, lot3, kitLot));
  }

  private StocksOnHand mockStocksOnHand2() {
    return new StocksOnHand(emptyList());
  }

  private List<ProductMovement> mockProductMovements1() {
    LotMovement movement1Lot1 = LotMovement.builder()
        .stockQuantity(10)
        .movementDetail(new MovementDetail(10, MovementType.ISSUE, "MATERNITY"))
        .lot(Lot.fromDatabase("lot1", java.sql.Date.valueOf("2023-12-31")))
        .build();
    ProductMovement movement1 = ProductMovement.builder()
        .productCode("test")
        .stockQuantity(10)
        .eventTime(EventTime
            .fromDatabase(Date.valueOf("2021-10-01"), "2021-09-15T01:23:45Z"))
        .movementDetail(new MovementDetail(10, MovementType.ISSUE, "MATERNITY"))
        .lotMovements(singletonList(movement1Lot1))
        .processedAt(Timestamp.valueOf("2021-11-01 01:23:45").toInstant())
        .build();
    LotMovement movement2Lot1 = LotMovement.builder()
        .stockQuantity(10)
        .movementDetail(new MovementDetail(10, MovementType.RECEIVE, "DISTRICT_DDM"))
        .lot(Lot.fromDatabase("lot1", java.sql.Date.valueOf("2023-12-31")))
        .build();
    ProductMovement movement2 = ProductMovement.builder()
        .productCode("test")
        .stockQuantity(10)
        .eventTime(EventTime
            .fromDatabase(Date.valueOf("2021-10-01"), "2021-10-01T01:23:45Z"))
        .movementDetail(new MovementDetail(10, MovementType.RECEIVE, "DISTRICT_DDM"))
        .lotMovements(singletonList(movement2Lot1))
        .processedAt(Timestamp.valueOf("2021-11-01 01:23:45").toInstant())
        .build();
    ProductMovement movement3 = ProductMovement.builder()
        .productCode("26A01")
        .stockQuantity(10)
        .eventTime(EventTime
            .fromDatabase(Date.valueOf("2021-10-31"), "2021-11-01T01:23:45Z"))
        .movementDetail(new MovementDetail(10, MovementType.RECEIVE, "DISTRICT_DDM"))
        .processedAt(Timestamp.valueOf("2021-11-01 01:23:45").toInstant())
        .build();
    return asList(movement1, movement2, movement3);
  }

}
