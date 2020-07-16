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

package org.siglus.siglusapi.domain;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.TestConsumptionServiceDto;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "rapid_test_consumption_line_items", schema = "siglusintegration")
public class TestConsumptionLineItem extends BaseEntity {

  private UUID requisitionId;

  private String project;

  private String outcome;

  private String service;

  private Integer value;

  public static List<TestConsumptionLineItem> from(List<TestConsumptionServiceDto> serviceDtos,
      UUID requisitionId) {
    List<TestConsumptionLineItem> lineItems = newArrayList();

    serviceDtos.forEach(serviceDto -> {
      String service = serviceDto.getService();
      serviceDto.getProjects()
          .forEach((projectKey, projectDto) ->
            projectDto.getOutcomes()
                .forEach((outcomeKey, outcomeDto) -> {
                  TestConsumptionLineItem lineItem = TestConsumptionLineItem.builder()
                          .value(outcomeDto.getValue())
                          .outcome(outcomeKey)
                          .project(projectKey)
                          .service(service)
                          .requisitionId(requisitionId)
                          .build();
                  lineItem.setId(outcomeDto.getTestConsumptionLineItemId());
                  lineItems.add(lineItem);
                }));
    });

    return lineItems;
  }
}
