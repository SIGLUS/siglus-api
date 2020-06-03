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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.javers.core.metamodel.annotation.TypeName;

@Entity
@Table(name = "shipment_drafts")
@TypeName("ShipmentDraft")
@AllArgsConstructor
@ToString
public class ShipmentDraft extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "orderid", unique = true)
  @Getter
  private Order order;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  private String notes;

  @OneToMany(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @JoinColumn(name = "shipmentdraftid", nullable = false)
  private List<ShipmentDraftLineItem> lineItems;

  // Constructor needed by framework. Use all args constructor to create new instance.
  private ShipmentDraft() {}

  /**
   * Gets a view of line items.
   */
  public List<ShipmentDraftLineItem> viewLineItems() {
    return Collections.unmodifiableList(
        lineItems.stream().map(ShipmentDraftLineItem::copy).collect(Collectors.toList()));
  }

  /**
   * Creates new instance based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}.
   * @return new instance of ShipmentDraft.
   */
  @NotNull
  public static ShipmentDraft newInstance(@NotNull Importer importer) {
    List<ShipmentDraftLineItem> items = new ArrayList<>(importer.getLineItems().size());
    importer.getLineItems().stream()
        .map(ShipmentDraftLineItem::newInstance)
        .forEach(items::add);

    ShipmentDraft inventoryItem = new ShipmentDraft(
        new Order(importer.getOrder().getId()),
        importer.getNotes(),
        items);
    inventoryItem.setId(importer.getId());

    return inventoryItem;
  }

  /**
   * Allows update existing draft.
   *
   * @param newDraft new draft to update from.
   */
  public void updateFrom(@NotNull ShipmentDraft newDraft) {
    order = newDraft.order;
    notes = newDraft.notes;

    updateLineItems(newDraft);
  }

  private void updateLineItems(ShipmentDraft newDraft) {
    List<ShipmentDraftLineItem> newLineItems = newDraft.viewLineItems();

    List<ShipmentDraftLineItem> updatedList = new ArrayList<>();
    for (ShipmentDraftLineItem newItem : newLineItems) {
      Optional<ShipmentDraftLineItem> existingItem = this.lineItems.stream()
          .filter(l -> l.getId().equals(newItem.getId()))
          .findFirst();

      if (existingItem.isPresent()) {
        ShipmentDraftLineItem existing = existingItem.get();
        existing.updateFrom(newItem);
        updatedList.add(existing);
      } else {
        newItem.setId(null);
        updatedList.add(newItem);
      }
    }

    this.lineItems.clear();
    this.lineItems.addAll(updatedList);
  }

  public interface Exporter {
    void setId(UUID id);

    void setOrder(Order order);

    void setNotes(String notes);
  }

  public interface Importer {
    UUID getId();

    Identifiable getOrder();

    String getNotes();

    List<ShipmentLineItem.Importer> getLineItems();
  }

  /**
   * Exports data from the given shipment draft to the instance that implement
   * {@link Exporter} interface.
   */
  public void export(Exporter exporter) {
    exporter.setId(getId());
    exporter.setOrder(order);
    exporter.setNotes(notes);
  }

}
