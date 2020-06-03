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

package org.openlmis.fulfillment.service;

import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_DTO_EXPANSION_ASSIGNMENT;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_DTO_EXPANSION_CAST;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_DTO_EXPANSION_HREF;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_ENCODING;
import static org.openlmis.fulfillment.service.request.RequestHelper.createEntity;
import static org.openlmis.fulfillment.service.request.RequestHelper.createUri;

import com.google.common.collect.Maps;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.fulfillment.service.request.RequestHeaders;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.util.converter.UuidConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

@Component
public class ObjReferenceExpander {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjReferenceExpander.class);
  private static final Map<String, Class> REF_RESOURCES = Maps.newHashMap();

  static {
    REF_RESOURCES.put("shipment", ShipmentDto.class);
    REF_RESOURCES.put("order", OrderDto.class);
  }

  @Autowired
  private AuthService authService;

  private BeanUtilsBean beanUtils;

  private RestOperations restTemplate = new RestTemplate();

  /**
   * Create an instance of the {@link BeanUtilsBean} and register custom converters with it.
   */
  @PostConstruct
  public void registerConverters() {
    beanUtils = BeanUtilsBean.getInstance();
    beanUtils.getConvertUtils().register(new UuidConverter(), UUID.class);
  }

  /**
   * Expands the DTO object. The requirement is that the field names in the {@code expands}
   * list exactly correspond to the field names in the passed DTO object. Moreover, those fields
   * need to extend the {@link ObjectReferenceDto}. If that's the case, this method will query the
   * URL from {@link ObjectReferenceDto#getHref()} and add the retrieved fields to the
   * representation.
   *
   * @param dto the DTO to expand
   * @param expands a set of field names from the passed DTO to expand
   */
  public void expandDto(Object dto, Set<String> expands) {
    if (expands == null) {
      return;
    }

    for (String expand : expands) {
      try {
        String[] elements = expand.split("\\.", 2);
        String field = elements[0];

        ObjectReferenceDto refDto = getObjectReferenceDto(dto, field);
        StringBuilder href = new StringBuilder(getHref(expand, refDto));

        if (elements.length >= 2) {
          href
              .append("?expand=")
              .append(UriUtils.encodeQueryParam(elements[1], StandardCharsets.UTF_8.name()));
        }

        Class<?> responseType = REF_RESOURCES.computeIfAbsent(field, key -> Map.class);
        Object refObj = retrieve(href.toString(), responseType);
        populate(dto, field, refObj);
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
        throw new ValidationException(ex, ERROR_DTO_EXPANSION_ASSIGNMENT, expand);
      } catch (UnsupportedEncodingException exp) {
        throw new ValidationException(exp, ERROR_ENCODING);
      }
    }
  }

  private ObjectReferenceDto getObjectReferenceDto(Object dto, String expand)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Object retrievedField = PropertyUtils.getProperty(dto, expand);

    if (!(retrievedField instanceof ObjectReferenceDto)) {
      throw new ValidationException(ERROR_DTO_EXPANSION_CAST, expand);
    }
    return (ObjectReferenceDto) retrievedField;
  }

  private String getHref(String expand, ObjectReferenceDto refDto) {
    String href = refDto.getHref();
    if (StringUtils.isBlank(href)) {
      throw new ValidationException(ERROR_DTO_EXPANSION_HREF, expand);
    }
    return href;
  }

  private <T> T retrieve(String href, Class<T> type) {
    HttpEntity<Object> entity =
        createEntity(RequestHeaders.init().setAuth(authService.obtainAccessToken()));
    try {
      return restTemplate
          .exchange(createUri(href), HttpMethod.GET, entity, type)
          .getBody();
    } catch (HttpStatusCodeException ex) {
      // We don't want to stop processing if the referenced instance does not exist.
      if (HttpStatus.NOT_FOUND == ex.getStatusCode()) {
        LOGGER.warn("The instance referenced under {} does not exist.", href);
        return null;
      }

      throw new DataRetrievalException(href, ex);
    }
  }

  private void populate(Object dto, String field, Object refObj)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    if (refObj instanceof Map) {
      Map map = (Map) refObj;

      if (MapUtils.isNotEmpty(map)) {
        ObjectReferenceDto refDto = getObjectReferenceDto(dto, field);
        beanUtils.populate(refDto, map);
      }
    } else if (null != refObj) {
      populate(dto, field, beanUtils.getPropertyUtils().describe(refObj));
    }
  }

}
