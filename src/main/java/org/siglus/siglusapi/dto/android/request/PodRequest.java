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

package org.siglus.siglusapi.dto.android.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.siglusapi.util.HashEncoder;

@Data
public class PodRequest {

  private LocalDate shippedDate;

  @NotNull
  private LocalDate receivedDate;

  @NotBlank
  private String deliveredBy;

  @NotBlank
  private String receivedBy;

  private String documentNo;

  private String originNumber;

  private Boolean isLocal;

  @JsonProperty("orderNumber")
  private String orderCode;

  private String programCode;

  @Valid
  private List<PodProductLineRequest> products;

  @JsonIgnore
  public String getSyncUpHash(UserDto user) {
    return HashEncoder.hash(programCode + user.getId() + user.getHomeFacilityId() + orderCode);
  }
}
