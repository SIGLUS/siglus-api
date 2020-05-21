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

package org.openlmis.referencedata.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.TypeName;
import org.openlmis.referencedata.dto.TradeItemClassificationDto;

/**
 * TradeItems represent branded/produced/physical products.  A TradeItem is used for Product's that
 * are made and then bought/sold/exchanged.  Unlike a {@link CommodityType} a TradeItem usually
 * has one and only one manufacturer and is shipped in exactly one primary package.
 *
 * <p>TradeItem's also may:
 * <ul>tr
 *   <li>have a GlobalTradeItemNumber</li>
 *   <li>a MSRP</li>
 * </ul>
 */
@Entity
@Table(name = "trade_items", schema = "referencedata")
@NoArgsConstructor
@TypeName("TradeItem")
public final class TradeItem extends BaseEntity {

  private String manufacturerOfTradeItem;

  @OneToMany(mappedBy = "tradeItem", cascade = CascadeType.ALL)
  @Getter
  @DiffIgnore
  private List<TradeItemClassification> classifications;

  @Column(unique = true, columnDefinition = "text")
  @Getter
  @Setter
  @Embedded
  private Gtin gtin;

  public TradeItem(String manufacturerOfTradeItem, List<TradeItemClassification> classifications) {
    this.manufacturerOfTradeItem = manufacturerOfTradeItem;
    this.classifications = classifications;
  }

  /**
   * A TradeItem can fulfill for the given product if the product is this trade item or if this
   * product's CommodityType is the given product.
   * @param product the product we'd like to fulfill for.
   * @return true if we can fulfill for the given product, false otherwise.
   */
  public boolean canFulfill(CommodityType product) {
    for (TradeItemClassification classification : classifications) {
      if (product.getClassificationSystem().equals(classification.getClassificationSystem())
          && product.getClassificationId().equals(classification.getClassificationId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Assigns a commodity type to this trade item - will associate this trade item
   * with the classification system of the provided commodity type.
   * @param commodityType the commodity type to associate with
   */
  public void assignCommodityType(CommodityType commodityType) {
    assignCommodityType(commodityType.getClassificationSystem(),
            commodityType.getClassificationId());
  }

  /**
   * Assigns to the classification system and classification id.
   * @param classificationSystem the classification system
   * @param classificationId the id of the classification system.
   */
  public void assignCommodityType(String classificationSystem, String classificationId) {
    TradeItemClassification existingClassification = findClassificationById(classificationId);

    if (existingClassification == null) {
      classifications.add(new TradeItemClassification(this, classificationSystem,
          classificationId));
    } else {
      existingClassification.setClassificationSystem(classificationSystem);
    }
  }

  TradeItemClassification findClassificationById(String classificationId) {
    for (TradeItemClassification classification : classifications) {
      if (classificationId.equals(classification.getClassificationId())) {
        return classification;
      }
    }
    return null;
  }

  /** Creates new instance based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of TradeItem.
   */
  public static TradeItem newInstance(Importer importer) {
    TradeItem tradeItem = new TradeItem();
    tradeItem.id = importer.getId();
    tradeItem.manufacturerOfTradeItem = importer.getManufacturerOfTradeItem();
    tradeItem.classifications = new ArrayList<>();
    if (importer.getGtin() != null) {
      tradeItem.gtin = new Gtin(importer.getGtin());
    }
    if (importer.getClassifications() != null) {
      importer.getClassifications().forEach(oe ->
          tradeItem.classifications.add(TradeItemClassification.newInstance(oe, tradeItem)));
    }

    return tradeItem;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setManufacturerOfTradeItem(manufacturerOfTradeItem);
    if (gtin != null) {
      exporter.setGtin(gtin.getGtin());
    }
    exporter.setClassifications(TradeItemClassificationDto.newInstance(classifications));
  }

  public interface Importer {
    UUID getId();

    String getManufacturerOfTradeItem();

    List<TradeItemClassificationDto> getClassifications();

    String getGtin();
  }

  public interface Exporter {
    void setId(UUID id);

    void setManufacturerOfTradeItem(String manufacturerOfTradeItem);

    void setClassifications(List<TradeItemClassificationDto> classifications);

    void setGtin(String gtin);
  }
}
