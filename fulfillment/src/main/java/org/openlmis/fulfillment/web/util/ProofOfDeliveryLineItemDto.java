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

package org.openlmis.fulfillment.web.util;

import static org.openlmis.fulfillment.service.ResourceNames.LOTS;
import static org.openlmis.fulfillment.service.ResourceNames.ORDERABLES;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.naming.VvmStatus;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"serviceUrl"})
public final class ProofOfDeliveryLineItemDto
    extends BaseDto
    implements ProofOfDeliveryLineItem.Importer, ProofOfDeliveryLineItem.Exporter {

  @Setter
  private String serviceUrl;

  @Getter
  private VersionObjectReferenceDto orderable;

  @Getter
  @Setter
  private ObjectReferenceDto lot;

  @Getter
  @Setter
  private Integer quantityAccepted;

  @Getter
  @Setter
  private Boolean useVvm;

  @Getter
  @Setter
  private VvmStatus vvmStatus;

  @Getter
  @Setter
  private Integer quantityRejected;

  @Getter
  @Setter
  private UUID rejectionReasonId;

  @Getter
  @Setter
  private String notes;

  /**
   * Create new instance of ProofOfDeliveryLineItemDto based of given
   * {@link ProofOfDeliveryLineItem}.
   *
   * @param lineItem instance of {@link ProofOfDeliveryLineItem}
   * @return new instance of ProofOfDeliveryLineItemDto.
   */
  public static ProofOfDeliveryLineItemDto newInstance(ProofOfDeliveryLineItem lineItem,
      OrderableDto orderableDto) {
    ProofOfDeliveryLineItemDto proofOfDeliveryLineItemDto = new ProofOfDeliveryLineItemDto();
    lineItem.export(proofOfDeliveryLineItemDto, orderableDto);

    return proofOfDeliveryLineItemDto;
  }

  @Override
  @JsonIgnore
  public VersionIdentityDto getOrderableIdentity() {
    return Optional
        .ofNullable(orderable)
        .map(item -> new VersionIdentityDto(orderable.getId(), orderable.getVersionNumber()))
        .orElse(null);
  }

  @JsonSetter("orderable")
  public void setOrderable(VersionObjectReferenceDto orderable) {
    this.orderable = orderable;
  }

  @Override
  @JsonIgnore
  public void setOrderable(OrderableDto orderable) {
    if (orderable != null) {
      this.orderable = new VersionObjectReferenceDto(orderable.getId(), serviceUrl,
          ORDERABLES, orderable.getVersionNumber());
    }
  }

  @Override
  @JsonIgnore
  public UUID getLotId() {
    return null == lot ? null : lot.getId();
  }

  @Override
  @JsonIgnore
  public void setLotId(UUID lotId) {
    if (null != lotId) {
      this.lot = ObjectReferenceDto.create(lotId, serviceUrl, LOTS);
    }
  }
}
