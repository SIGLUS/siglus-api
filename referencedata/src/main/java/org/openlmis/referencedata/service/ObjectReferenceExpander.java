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

package org.openlmis.referencedata.service;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.openlmis.referencedata.util.messagekeys.DtoExpansionMessageKeys.ERROR;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Set;
import org.apache.commons.beanutils.PropertyUtils;
import org.openlmis.referencedata.domain.BaseEntity;
import org.openlmis.referencedata.dto.ObjectReferenceDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.springframework.stereotype.Component;

@Component
public class ObjectReferenceExpander {

  private static final String EXPORT_METHOD_NAME = "export";
  private static final String EXPORTER_INTERFACE_NAME = "Exporter";

  /**
   * Expands the DTO object. The requirement is that the field names in the {@code expands}
   * list exactly correspond to the field names in the passed DTO object. Moreover, those fields
   * need to extend the {@link ObjectReferenceDto}. If that's the case, this method will
   * use export method from entity objects and populates dto fields.
   *
   * @param dto     the DTO to expand
   * @param entity  entity object to get data from
   * @param expands a set of field names from the passed DTO to expand
   */
  public void expandDto(Object dto, BaseEntity entity, Set<String> expands) {
    if (isEmpty(expands)) {
      return;
    }
    expands.forEach(e -> expand(dto, entity, e));
  }

  private void expand(Object dto, Object entity, String expand) {
    String propertyName = null;

    try {
      String[] parts = expand.split("\\.", 2);
      propertyName = parts[0];

      Object entityProperty = PropertyUtils.getProperty(entity, propertyName);

      if (entityProperty == null) {
        return;
      }

      if (entityProperty instanceof Collection) {
        expandCollection(dto, entityProperty, propertyName, parts.length == 2 ? parts[1] : null);
      } else {
        expandField(dto, entityProperty, propertyName, parts.length == 2 ? parts[1] : null);
      }
    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException
        | InstantiationException | NoSuchFieldException e) {
      throw new ValidationMessageException(e, ERROR, propertyName, dto.getClass());
    }
  }

  private void expandCollection(Object dto, Object entity, String propertyName, String expand)
      throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException,
      InvocationTargetException, InstantiationException {
    Class dtoClass = getGenericClassFromCollection(dto, propertyName);
    Object dtoProperty = PropertyUtils.getProperty(dto, propertyName);

    Collection collection = ((Collection) dtoProperty);
    collection.clear();

    for (Object e : (Collection) entity) {
      Object dtoInstance = dtoClass.newInstance();
      getExportMethod(e, dtoClass).invoke(e, dtoInstance);
      collection.add(dtoInstance);

      if (isNotBlank(expand)) {
        expand(dtoInstance, entity, expand);
      }
    }
  }

  private void expandField(Object dto, Object entity, String propertyName, String expand)
      throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    Object dtoProperty = PropertyUtils.getProperty(dto, propertyName);
    getExportMethod(entity, dtoProperty.getClass()).invoke(entity, dtoProperty);

    if (isNotBlank(expand)) {
      expand(dtoProperty, entity, expand);
    }
  }

  private Class getExporter(Class<?>[] asd) {
    Class importer = null;
    for (Class inter : asd) {
      if (inter.getName().contains(EXPORTER_INTERFACE_NAME)) {
        importer = inter;
      }
    }
    return importer;
  }

  private Method getExportMethod(Object entity, Class dtoClass) throws NoSuchMethodException {
    return entity.getClass().getMethod(EXPORT_METHOD_NAME, getExporter(dtoClass.getInterfaces()));
  }

  private Class getGenericClassFromCollection(Object dto, String propertyName)
      throws NoSuchFieldException {
    return (Class) ((ParameterizedType) dto.getClass()
        .getDeclaredField(propertyName)
        .getGenericType())
        .getActualTypeArguments()[0];
  }
}
