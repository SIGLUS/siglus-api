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

package org.openlmis.referencedata.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.referencedata.domain.TradeItemClassification;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
public class TradeItemClassificationDto implements TradeItemClassification.Exporter,
    TradeItemClassification.Importer {

  private String classificationSystem;

  private String classificationId;

  /**
   * Creates new list based on given {@link TradeItemClassification} iterable.
   *
   * @param classifications iterable of TradeItemClassification.
   */
  public static List<TradeItemClassificationDto> newInstance(
      Iterable<TradeItemClassification> classifications) {
    List<TradeItemClassificationDto> classificationDtos = new ArrayList<>();
    classifications.forEach(c -> classificationDtos.add(newInstance(c)));
    return classificationDtos;
  }

  /**
   * Creates new instance based on given {@link TradeItemClassification}.
   *
   * @param tic instance of TradeItemClassification.
   * @return new instance of TradeItemClassificationDto.
   */
  public static TradeItemClassificationDto newInstance(TradeItemClassification tic) {
    if (tic == null) {
      return null;
    }
    TradeItemClassificationDto classificationDto = new TradeItemClassificationDto();
    tic.export(classificationDto);

    return classificationDto;
  }
}
