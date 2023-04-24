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

package org.siglus.siglusapi.dto.android.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
public class ProductResponse {

  private String productCode;
  private Boolean active;
  private Boolean archived;
  private String fullProductName;
  private String description;
  private Long netContent;
  private Long packRoundingThreshold;
  private Boolean roundToZero;
  private String category;
  private String programCode;
  private String additionalProgramCode;
  private Boolean isKit;
  private List<ProductChildResponse> children = Collections.emptyList();
  private Boolean isBasic;
  private Boolean isNos;
  private Boolean isHiv;
  private Instant lastUpdated;
  private BigDecimal pricePerPack;
  private String reportName;
  private String unit;
}