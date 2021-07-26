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

package org.siglus.siglusapi.web.android;

import static org.mockito.Matchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.service.android.SiglusMeService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class SiglusMeControllerProofOfDeliveryMvcTest extends FileBasedTest {

  private MockMvc mockMvc;

  @InjectMocks
  private SiglusMeService service;

  @Before
  public void setup() {
    SiglusMeController controller = new SiglusMeController(service);
    this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    // mock pods
    // mock lots
    // mock order
    // mock product
    // mock reason
  }

  @Test
  public void shouldReturnAllWhenGetPodsGivenNoParams() throws Exception {
    // given
    RequestBuilder request = get("/api/siglusapi/android/me/facility/pods")
        .contentType(MediaType.APPLICATION_JSON)
        .characterEncoding("utf-8");

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    resultActions.andExpect(status().isOk())
        .andExpect(jsonPath("[0].confirmDate").value("2020-10-01"))
        .andExpect(jsonPath("[0].deliveredBy").value("qla"))
        .andExpect(jsonPath("[0].receivedBy").value("zjj"))
        .andExpect(jsonPath("[0].order.code").value("ORDER-AS20JF"))
        .andExpect(jsonPath("[0].order.supplyFacilityName").value("Centro de Saude de ntopa"))
        .andExpect(jsonPath("[0].order.date").value("2020-09-02"))
        .andExpect(jsonPath("[0].order.status").value("RECEIVED"))
        .andExpect(jsonPath("[0].order.requisition.number").value("RNR-NO01050119-0"))
        .andExpect(jsonPath("[0].order.requisition.isEmergency").value("false"))
        .andExpect(jsonPath("[0].order.requisition.programCode").value("VC"))
        .andExpect(jsonPath("[0].order.requisition.startDate").value("2020-07-21"))
        .andExpect(jsonPath("[0].order.requisition.endDate").value("2020-08-20"))
        .andExpect(jsonPath("[0].products[0].code").value("22A01"))
        .andExpect(jsonPath("[0].products[0].orderedQuantity").value(20))
        .andExpect(jsonPath("[0].products[0].partialFulfilled").value(0))
        .andExpect(jsonPath("[0].products[0].lots[0].lot.code").value("SME-LOTE-22A01-062023"))
        .andExpect(jsonPath("[0].products[0].lots[0].lot.expirationDate").value("2023-06-30"))
        .andExpect(jsonPath("[0].products[0].lots[0].shippedQuantity").value(20))
        .andExpect(jsonPath("[0].products[0].lots[0].acceptedQuantity").value(10))
        .andExpect(jsonPath("[0].products[0].lots[0].rejectedReason").value("reject"))
        .andExpect(jsonPath("[0].products[0].lots[0].notes").value("123"))
        .andExpect(jsonPath("[1].confirmDate").value("2020-11-01"))
        .andExpect(jsonPath("[1].deliveredBy").value("qla"))
        .andExpect(jsonPath("[1].receivedBy").value("zjj"))
        .andExpect(jsonPath("[1].order.code").value("ORDER-AS21JF"))
        .andExpect(jsonPath("[1].order.supplyFacilityName").value("Centro de Saude de ntopa"))
        .andExpect(jsonPath("[1].order.date").value("2020-10-02"))
        .andExpect(jsonPath("[1].order.status").value("RECEIVED"))
        .andExpect(jsonPath("[1].order.requisition.number").value("RNR-NO01050120-0"))
        .andExpect(jsonPath("[1].order.requisition.isEmergency").value("false"))
        .andExpect(jsonPath("[1].order.requisition.programCode").value("VC"))
        .andExpect(jsonPath("[1].order.requisition.startDate").value("2020-08-21"))
        .andExpect(jsonPath("[1].order.requisition.endDate").value("2020-09-20"))
        .andExpect(jsonPath("[1].products[0].code").value("22A01"))
        .andExpect(jsonPath("[1].products[0].orderedQuantity").value(20))
        .andExpect(jsonPath("[1].products[0].partialFulfilled").value(0))
        .andExpect(jsonPath("[1].products[0].lots[0].lot.code").value("SME-LOTE-22A01-062023"))
        .andExpect(jsonPath("[1].products[0].lots[0].lot.expirationDate").value("2023-06-30"))
        .andExpect(jsonPath("[1].products[0].lots[0].shippedQuantity").value(20))
        .andExpect(jsonPath("[1].products[0].lots[0].acceptedQuantity").value(20))
        .andExpect(jsonPath("[1].products[0].lots[0].rejectedReason").value(isNull()))
        .andExpect(jsonPath("[1].products[0].lots[0].notes").value("123"))
        .andExpect(jsonPath("[2].confirmDate").value(isNull()))
        .andExpect(jsonPath("[2].deliveredBy").value(isNull()))
        .andExpect(jsonPath("[2].receivedBy").value(isNull()))
        .andExpect(jsonPath("[2].order.code").value("ORDER-AS22JF"))
        .andExpect(jsonPath("[2].order.supplyFacilityName").value("Centro de Saude de ntopa"))
        .andExpect(jsonPath("[2].order.date").value("2020-11-02"))
        .andExpect(jsonPath("[2].order.status").value("SHIPPED"))
        .andExpect(jsonPath("[2].order.requisition.number").value("RNR-NO01050121-0"))
        .andExpect(jsonPath("[2].order.requisition.isEmergency").value("false"))
        .andExpect(jsonPath("[2].order.requisition.programCode").value("VC"))
        .andExpect(jsonPath("[2].order.requisition.startDate").value("2020-09-21"))
        .andExpect(jsonPath("[2].order.requisition.endDate").value("2020-10-20"))
        .andExpect(jsonPath("[2].products[0].code").value("22A01"))
        .andExpect(jsonPath("[2].products[0].orderedQuantity").value(20))
        .andExpect(jsonPath("[2].products[0].partialFulfilled").value(0))
        .andExpect(jsonPath("[2].products[0].lots[0].lot.code").value("SME-LOTE-22A01-062023"))
        .andExpect(jsonPath("[2].products[0].lots[0].lot.expirationDate").value("2023-06-30"))
        .andExpect(jsonPath("[2].products[0].lots[0].shippedQuantity").value(isNull()))
        .andExpect(jsonPath("[2].products[0].lots[0].acceptedQuantity").value(isNull()))
        .andExpect(jsonPath("[2].products[0].lots[0].rejectedReason").value(isNull()))
        .andExpect(jsonPath("[2].products[0].lots[0].notes").value(isNull()));
  }

  @Test
  public void shouldReturnAllWhenGetPodsGivenStartDateAndInitiatedOnly() throws Exception {
    // given
    RequestBuilder request = get("/api/siglusapi/android/me/facility/pods?startDate=2020-09-11&initiatedOnly=true")
        .contentType(MediaType.APPLICATION_JSON)
        .characterEncoding("utf-8");

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    // verify using the params
  }

  @Test
  public void shouldReturnAllWhenGetPodsGivenStartDate() throws Exception {
    // given
    RequestBuilder request = get("/api/siglusapi/android/me/facility/pods?startDate=2020-09-11")
        .contentType(MediaType.APPLICATION_JSON)
        .characterEncoding("utf-8");

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    // verify using the params
  }


  @Test
  public void shouldReturnAllWhenGetPodsGivenInitiatedOnly() throws Exception {
    // given
    RequestBuilder request = get("/api/siglusapi/android/me/facility/pods?initiatedOnly=true")
        .contentType(MediaType.APPLICATION_JSON)
        .characterEncoding("utf-8");

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    // verify using the params
  }

}
