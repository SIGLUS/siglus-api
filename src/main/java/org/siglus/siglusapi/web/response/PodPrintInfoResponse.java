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

package org.siglus.siglusapi.web.response;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class PodPrintInfoResponse {

  private String fileName;

  private String supplier;
  private String client;
  private String supplierDistrict;
  private String supplierProvince;
  private UUID requisitionId;
  // Date that the HF Role3 internally approve the requisition
  private ZonedDateTime requisitionDate;
  // Date that the supplier fulfills the order
  private Date issueVoucherDate;

  private String deliveredBy;
  private String receivedBy;
  private LocalDate receivedDate;

  private List<LineItemInfo> lineItems;

  @Data
  public static class LineItemInfo {

    private String productCode;
    private String productName;
    private UUID lotId;
    private String lotCode;
    private LocalDate lotExpirationDate;

    private Long requestedQuantity;
    private Long orderedQuantity;
    private Long receivedQuantity;
  }
}
