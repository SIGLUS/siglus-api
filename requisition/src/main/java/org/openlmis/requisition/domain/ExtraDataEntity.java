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

package org.openlmis.requisition.domain;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.openlmis.requisition.domain.converter.ExtraDataConverter;

@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ExtraDataEntity {

  // Because of https://openlmis.atlassian.net/browse/OLMIS-5143
  // the type of a key in the extraData map has to be changed to Object.
  @Getter
  @Convert(converter = ExtraDataConverter.class)
  @Column(name = "extradata", columnDefinition = "jsonb")
  private Map<String, Object> extraData = new HashMap<>();

  public ExtraDataEntity(Map<String, Object> importer) {
    updateFrom(importer);
  }

  public static ExtraDataEntity defaultEntity(ExtraDataEntity entity) {
    return entity == null ? new ExtraDataEntity() : entity;
  }

  public boolean containsKey(String key) {
    return extraData.containsKey(key);
  }

  public Object get(String key) {
    return extraData.get(key);
  }

  public Object put(String key, Object value) {
    return extraData.put(key, value);
  }

  /**
   * Update this from another.
   */
  public void updateFrom(Map<String, Object> importer) {
    extraData.clear();
    Optional
        .ofNullable(importer)
        .ifPresent(extraData::putAll);
  }

  /**
   * Exports current state of object.
   */
  public void export(ExtraDataExporter exporter) {
    Map<String, Object> newExtraData = Maps.newHashMap();
    Optional.ofNullable(extraData).ifPresent(newExtraData::putAll);

    exporter.setExtraData(newExtraData);
  }

  public interface ExtraDataExporter {

    void setExtraData(Map<String, Object> extraData);

  }

  public interface ExtraDataImporter {

    Map<String, Object> getExtraData();

  }

}
