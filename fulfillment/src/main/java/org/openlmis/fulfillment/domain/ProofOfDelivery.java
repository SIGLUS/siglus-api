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

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.TypeName;
import org.openlmis.fulfillment.i18n.MessageKeys;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;

@Entity
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Table(name = "proofs_of_delivery", schema = "fulfillment")
@TypeName("ProofOfDelivery")
public class ProofOfDelivery extends BaseEntity {

  @OneToOne
  @JoinColumn(name = "shipmentId", nullable = false)
  @Getter
  @DiffIgnore
  private Shipment shipment;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Getter(AccessLevel.PACKAGE)
  private ProofOfDeliveryStatus status;

  @OneToMany(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @JoinColumn(name = "proofOfDeliveryId", nullable = false)
  @Getter
  private List<ProofOfDeliveryLineItem> lineItems;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  private String receivedBy;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter(AccessLevel.PACKAGE)
  private String deliveredBy;

  @Getter
  private LocalDate receivedDate;

  private ProofOfDelivery(Shipment shipment, List<ProofOfDeliveryLineItem> lineItems) {
    this(shipment, ProofOfDeliveryStatus.INITIATED, lineItems, null, null, null);
  }

  public UUID getProgramId() {
    return shipment.getProgramId();
  }

  public UUID getReceivingFacilityId() {
    return shipment.getReceivingFacilityId();
  }

  public UUID getSupplyingFacilityId() {
    return shipment.getSupplyingFacilityId();
  }

  /**
   * Copy values of attributes into new or updated ProofOfDelivery.
   *
   * @param proofOfDelivery ProofOfDelivery with new values.
   */
  public void updateFrom(ProofOfDelivery proofOfDelivery) {
    this.receivedBy = proofOfDelivery.receivedBy;
    this.deliveredBy = proofOfDelivery.deliveredBy;
    this.receivedDate = proofOfDelivery.receivedDate;
    updateLines(proofOfDelivery.lineItems);
  }

  /**
   * Try to confirm this Proof of delivery.
   */
  public void confirm(Map<VersionIdentityDto, OrderableDto> orderables) {
    Validations.throwIfBlank(deliveredBy, "deliveredBy");
    Validations.throwIfBlank(receivedBy, "receivedBy");
    Validations.throwIfNull(receivedDate, "receivedDate");

    for (ProofOfDeliveryLineItem lineItem : lineItems) {
      shipment
          .getLineItems()
          .stream()
          .filter(shipped -> shipped.getOrderable().equals(lineItem.getOrderable())
              && Objects.equals(shipped.getLotId(), lineItem.getLotId()))
          .findFirst()
          .ifPresent(shipped -> lineItem.validate(shipped.getQuantityShipped(), orderables));
    }

    status = ProofOfDeliveryStatus.CONFIRMED;
  }

  public boolean isConfirmed() {
    return status == ProofOfDeliveryStatus.CONFIRMED;
  }

  private void updateLines(Collection<ProofOfDeliveryLineItem> newLineItems) {
    if (null == newLineItems) {
      return;
    }

    if (null == lineItems) {
      lineItems = new ArrayList<>();
    }

    for (ProofOfDeliveryLineItem item : newLineItems) {
      lineItems
          .stream()
          .filter(l -> l.getId().equals(item.getId()))
          .findFirst()
          .ifPresent(existing -> existing.updateFrom(item));

    }
  }

  /**
   * Create instance of ProofOfDelivery based on given {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return instance of ProofOfDelivery.
   */
  public static ProofOfDelivery newInstance(Importer importer) {
    validateLineItems(importer.getLineItems());

    List<ProofOfDeliveryLineItem> items = importer
        .getLineItems()
        .stream()
        .map(ProofOfDeliveryLineItem::newInstance)
        .collect(Collectors.toList());

    Shipment shipment = new Shipment(null, null, null, null, null);
    shipment.setId(importer.getShipment().getId());

    ProofOfDelivery proofOfDelivery = new ProofOfDelivery(
        shipment, importer.getStatus(), items,
        importer.getReceivedBy(), importer.getDeliveredBy(),
        importer.getReceivedDate()
    );
    proofOfDelivery.setId(importer.getId());

    return proofOfDelivery;
  }

  /**
   * Create instance of ProofOfDelivery based on given {@link Shipment} and information about using
   * vvm status by orderables.
   *
   * @param shipment instance of {@link Shipment}
   * @return instance of ProofOfDelivery.
   */
  public static ProofOfDelivery newInstance(Shipment shipment) {
    List<ProofOfDeliveryLineItem> items = shipment
        .getLineItems()
        .stream()
        .filter(ShipmentLineItem::isShipped)
        .map(ProofOfDeliveryLineItem::new)
        .collect(Collectors.toList());

    return new ProofOfDelivery(shipment, items);
  }

  private static void validateLineItems(List<?> lineItems) {
    if (isEmpty(lineItems)) {
      throw new ValidationException(MessageKeys.PROOF_OF_DELIVERY_LINE_ITEMS_REQUIRED);
    }
  }

  /**
   * Returns a set of all orderable identities in this proof of delivery.
   */
  public Set<VersionEntityReference> getAllOrderables() {
    return Optional
        .ofNullable(lineItems)
        .orElse(Collections.emptyList())
        .stream()
        .map(ProofOfDeliveryLineItem::getOrderable)
        .collect(Collectors.toSet());
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setShipment(shipment);
    exporter.setStatus(status);
    exporter.setReceivedBy(receivedBy);
    exporter.setDeliveredBy(deliveredBy);
    exporter.setReceivedDate(receivedDate);
  }

  public interface Exporter {

    void setId(UUID id);

    void setShipment(Shipment shipment);

    void setStatus(ProofOfDeliveryStatus status);

    void setReceivedBy(String receivedBy);

    void setDeliveredBy(String deliveredBy);

    void setReceivedDate(LocalDate receivedDate);

  }

  public interface Importer {

    UUID getId();

    Identifiable getShipment();

    ProofOfDeliveryStatus getStatus();

    List<ProofOfDeliveryLineItem.Importer> getLineItems();

    String getReceivedBy();

    String getDeliveredBy();

    LocalDate getReceivedDate();

  }
}
