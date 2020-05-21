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

package org.openlmis.referencedata.web.csv.model;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openlmis.referencedata.dto.BaseDto;

/**
 * This class contains a list of {@link ModelField} for each dto class. It only retrieve class
 * fields on the first time. Any additional execution will simple return cached list. This should
 * slightly speed up the process of creating {@link ModelClass}.
 */
class ModelClassHelper {
  private static final Map<Class<?>, List<ModelField>> MAP = Maps.newConcurrentMap();

  static List<ModelField> getModelFields(Class<? extends BaseDto> clazz) {
    return MAP.computeIfAbsent(clazz, ModelClassHelper::fieldsWithImportFieldAnnotation);
  }

  private static List<ModelField> fieldsWithImportFieldAnnotation(Class clazz) {
    return Arrays
        .stream(clazz.getDeclaredFields())
        .filter(field -> field.isAnnotationPresent(ImportField.class))
        .map(field -> new ModelField(field, field.getAnnotation(ImportField.class)))
        .collect(Collectors.toList());
  }
}
