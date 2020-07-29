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

package org.openlmis.fulfillment.domain;

import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_INCORRECT_QUANTITIES;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_INCORRECT_VVM_STATUS;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_MISSING_REASON;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.javers.core.metamodel.annotation.TypeName;
import org.openlmis.fulfillment.domain.naming.VvmStatus;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;

@Entity
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Table(name = "proof_of_delivery_line_items", schema = "fulfillment")
@TypeName("ProofOfDeliveryLineItem")
public class ProofOfDeliveryLineItem extends BaseEntity {

  @Getter
  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(name = "orderableId")),
      @AttributeOverride(name = "versionNumber", column = @Column(name = "orderableVersionNumber"))
  })
  @Embedded
  private VersionEntityReference orderable;

  @Type(type = UUID_TYPE)
  @Getter(AccessLevel.PACKAGE)
  private UUID lotId;

  // [SIGLUS change start]
  // [change reason]: #401 AC5 If accepted quantity greater than 0 ,
  //                  it can have stock event record.
  // @Getter(AccessLevel.PACKAGE)
  @Getter
  // [SIGLUS change end]
  private Integer quantityAccepted;

  @Enumerated(EnumType.STRING)
  @Getter(AccessLevel.PACKAGE)
  private VvmStatus vvmStatus;

  @Getter(AccessLevel.PACKAGE)
  private Integer quantityRejected;

  @Getter(AccessLevel.PACKAGE)
  private UUID rejectionReasonId;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter(AccessLevel.PACKAGE)
  private String notes;

  ProofOfDeliveryLineItem(ShipmentLineItem shipmentLineItem) {
    this(
        shipmentLineItem.getOrderable(), shipmentLineItem.getLotId(),
        null, null, null, null, null
    );
  }

  /**
   * Copy values of attributes into new or updated ProofOfDeliveryLineItem.
   *
   * @param proofOfDeliveryLineItem ProofOfDeliveryLineItem with new values.
   */
  void updateFrom(ProofOfDeliveryLineItem proofOfDeliveryLineItem) {
    this.quantityAccepted = proofOfDeliveryLineItem.quantityAccepted;
    this.vvmStatus = proofOfDeliveryLineItem.vvmStatus;
    this.quantityRejected = proofOfDeliveryLineItem.quantityRejected;
    this.rejectionReasonId = proofOfDeliveryLineItem.rejectionReasonId;
    this.notes = proofOfDeliveryLineItem.notes;
  }

  /**
   * Validate if this line item has correct values. The following validations will be done:
   * <ul>
   * <li><strong>quantityAccepted</strong> - must be zero or greater than zero</li>
   * <li><strong>quantityRejected</strong> - must be zero or greater than zero</li>
   * <li>if <strong>quantityAccepted</strong> is greater than zero and <strong>useVvm</strong> flag
   * is set, the <strong>vvmStatus</strong> must be less or equal to two</li>
   * <li>if <strong>quantityRejected</strong> is greater than zero, reason id must be provided</li>
   * <li>sum of <strong>quantityAccepted</strong> and <strong>quantityRejected</strong> must be
   * equals to <strong>quantityShipped</strong></li>
   * </ul>
   *
   * @param quantityShipped this value should be from related shipment line item.
   * @throws ValidationException if any validation does not match.
   */
  void validate(Long quantityShipped, Map<VersionIdentityDto, OrderableDto> orderables) {
    Validations.throwIfLessThanZeroOrNull(quantityAccepted, "quantityAccepted");
    Validations.throwIfLessThanZeroOrNull(quantityRejected, "quantityRejected");

    boolean useVvm = getUseVvmFromOrderables(orderables);

    if (quantityAccepted > 0
        && useVvm
        && (null == vvmStatus || vvmStatus.isGreaterThan(2))) {
      throw new ValidationException(ERROR_INCORRECT_VVM_STATUS);
    }

    if (quantityRejected > 0 && null == rejectionReasonId) {
      throw new ValidationException(ERROR_MISSING_REASON);
    }

    if (quantityAccepted + quantityRejected != Math.toIntExact(quantityShipped)) {
      throw new ValidationException(ERROR_INCORRECT_QUANTITIES);
    }
  }

  private Boolean getUseVvmFromOrderables(Map<VersionIdentityDto, OrderableDto> orderables) {
    OrderableDto orderableDto = orderables.get(new VersionIdentityDto(this.orderable));
    return orderableDto.useVvm();
  }

  /**
   * Create new instance of ProofOfDeliveryLineItem based on given
   * {@link Importer}.
   * @param importer instance of {@link Importer}
   * @return instance of ProofOfDeliveryLineItem.
   */
  static ProofOfDeliveryLineItem newInstance(Importer importer) {
    VersionIdentityDto orderableDto = importer.getOrderableIdentity();

    VersionEntityReference orderable = Optional
        .ofNullable(orderableDto)
        .map(item -> new VersionEntityReference(orderableDto.getId(),
        orderableDto.getVersionNumber()))
        .orElse(null);

    ProofOfDeliveryLineItem lineItem = new ProofOfDeliveryLineItem(
        orderable, importer.getLotId(), importer.getQuantityAccepted(),
        importer.getVvmStatus(), importer.getQuantityRejected(),
        importer.getRejectionReasonId(), importer.getNotes()
    );
    lineItem.setId(importer.getId());

    return lineItem;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter, OrderableDto orderableDto) {
    exporter.setId(id);
    exporter.setOrderable(orderableDto);
    exporter.setLotId(lotId);
    exporter.setQuantityAccepted(quantityAccepted);
    exporter.setUseVvm(orderableDto.useVvm());
    exporter.setVvmStatus(vvmStatus);
    exporter.setQuantityRejected(quantityRejected);
    exporter.setRejectionReasonId(rejectionReasonId);
    exporter.setNotes(notes);
  }

  public interface Importer {
    UUID getId();

    VersionIdentityDto getOrderableIdentity();

    UUID getLotId();

    Integer getQuantityAccepted();

    VvmStatus getVvmStatus();

    Integer getQuantityRejected();

    UUID getRejectionReasonId();

    String getNotes();

  }

  public interface Exporter {
    void setId(UUID id);

    void setOrderable(OrderableDto orderableDto);

    void setLotId(UUID lotId);

    void setQuantityAccepted(Integer quantityAccepted);

    void setUseVvm(Boolean useVvm);

    void setVvmStatus(VvmStatus vvmStatus);

    void setQuantityRejected(Integer quantityRejected);

    void setRejectionReasonId(UUID rejectionReasonId);

    void setNotes(String notes);

  }

}
