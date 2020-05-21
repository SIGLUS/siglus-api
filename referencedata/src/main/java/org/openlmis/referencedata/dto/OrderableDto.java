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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.referencedata.domain.Dispensable;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.measurement.TemperatureMeasurement;
import org.openlmis.referencedata.domain.measurement.VolumeMeasurement;
import org.openlmis.referencedata.repository.OrderableRepository;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class OrderableDto extends BaseDto implements Orderable.Importer,
    Orderable.Exporter {

  private String productCode;

  private DispensableDto dispensable;

  private String fullProductName;

  private String description;

  private Long netContent;

  private Long packRoundingThreshold;

  private Boolean roundToZero;

  private Set<ProgramOrderableDto> programs;

  private Set<OrderableChildDto> children;

  private Map<String, String> identifiers;

  private Map<String, Object> extraData;

  private MetadataDto meta = new MetadataDto();

  private TemperatureMeasurementDto minimumTemperature;

  private TemperatureMeasurementDto maximumTemperature;

  private VolumeMeasurementDto inBoxCubeDimension;

  @JsonIgnore
  private OrderableRepository orderableRepository;

  /**
   * Create new set of OrderableDto based on given iterable of {@link Orderable}.
   *
   * @param orderables list of {@link Orderable}
   * @return new list of OrderableDto.
   */
  public static List<OrderableDto> newInstance(Iterable<Orderable> orderables) {
    List<OrderableDto> orderableDtos = new LinkedList<>();
    orderables.forEach(oe -> orderableDtos.add(newInstance(oe)));
    return orderableDtos;
  }

  /**
   * Creates new instance based on given {@link Orderable}.
   *
   * @param po instance of Orderable.
   * @return new instance of OrderableDto.
   */
  public static OrderableDto newInstance(Orderable po) {
    if (po == null) {
      return null;
    }
    OrderableDto orderableDto = new OrderableDto();
    po.export(orderableDto);

    return orderableDto;
  }

  @JsonSetter("dispensable")
  public void setDispensable(DispensableDto dispensable) {
    this.dispensable = dispensable;
  }

  @Override
  public void setDispensable(Dispensable dispensable) {
    this.dispensable = new DispensableDto();
    dispensable.export(this.dispensable);
  }

  @Override
  @JsonIgnore
  public Long getVersionNumber() {
    if (null == orderableRepository) {
      return 1L;
    } else {
      Orderable latestOrderable = orderableRepository
          .findFirstByIdentityIdOrderByIdentityVersionNumberDesc(getId());
      return latestOrderable.getVersionNumber();
    }
  }

  @Override
  public void setVersionNumber(Long versionNumber) {
    meta.setVersionNumber(versionNumber);
  }

  @Override
  public void setLastUpdated(ZonedDateTime lastUpdated) {
    meta.setLastUpdated(lastUpdated);
  }

  @JsonSetter("minimumTemperature")
  public void setMinimumTemperature(
          TemperatureMeasurementDto minimumTemperature) {
    this.minimumTemperature = minimumTemperature;
  }

  public void setMinimumTemperature(
          TemperatureMeasurement minimumTemperature) {
    this.minimumTemperature = new TemperatureMeasurementDto();
    minimumTemperature.export(this.minimumTemperature);
  }

  @JsonSetter("maximumTemperature")
  public void setMaximumTemperature(
          TemperatureMeasurementDto maximumTemperature) {
    this.maximumTemperature = maximumTemperature;
  }

  public void setMaximumTemperature(
          TemperatureMeasurement maximumTemperature) {
    this.maximumTemperature = new TemperatureMeasurementDto();
    maximumTemperature.export(this.maximumTemperature);
  }

  @JsonSetter("inBoxCubeDimension")
  public void setInBoxCubeDimension(VolumeMeasurementDto inBoxCubeDimension) {
    this.inBoxCubeDimension = inBoxCubeDimension;
  }

  @Override
  public void setInBoxCubeDimension(VolumeMeasurement inBoxCubeDimension) {
    this.inBoxCubeDimension = new VolumeMeasurementDto();
    inBoxCubeDimension.export(this.inBoxCubeDimension);
  }
}
