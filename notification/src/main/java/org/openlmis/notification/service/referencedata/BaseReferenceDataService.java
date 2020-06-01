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

package org.openlmis.notification.service.referencedata;

import static org.openlmis.notification.service.request.RequestHelper.createUri;

import java.util.UUID;
import org.openlmis.notification.service.BaseCommunicationService;
import org.openlmis.notification.service.request.RequestParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

public abstract class BaseReferenceDataService<T> extends BaseCommunicationService<T> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Value("${referencedata.url}")
  private String referenceDataUrl;

  /**
   * Return one object from Reference data service.
   *
   * @param id UUID of requesting object.
   * @return Requesting reference data object.
   */
  public T findOne(UUID id) {
    String url = getServiceUrl() + getUrl() + id;

    try {
      ResponseEntity<T> responseEntity = restTemplate.exchange(
          buildUri(url), HttpMethod.GET, createEntity(), getResultClass());
      return responseEntity.getBody();
    } catch (HttpStatusCodeException ex) {
      // rest template will handle 404 as an exception, instead of returning null
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        logger.warn("{} with id {} does not exist. ", getResultClass().getSimpleName(), id);
        return null;
      } else {
        throw buildDataRetrievalException(ex);
      }
    }
  }

  <P> P get(Class<P> type, String resourceUrl, RequestParameters parameters) {
    String url = getServiceUrl() + getUrl() + resourceUrl;

    ResponseEntity<P> response = restTemplate.exchange(createUri(url, parameters), HttpMethod.GET,
            createEntity(), type);

    return response.getBody();
  }

  protected String getServiceUrl() {
    return referenceDataUrl;
  }

  protected abstract String getUrl();

  protected abstract Class<T> getResultClass();

  protected abstract Class<T[]> getArrayResultClass();
}
