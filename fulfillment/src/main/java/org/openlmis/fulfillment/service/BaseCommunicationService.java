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

import static org.openlmis.fulfillment.service.request.RequestHelper.createUri;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openlmis.fulfillment.service.request.RequestHeaders;
import org.openlmis.fulfillment.service.request.RequestHelper;
import org.openlmis.fulfillment.service.request.RequestParameters;
import org.openlmis.fulfillment.util.DynamicPageTypeReference;
import org.openlmis.fulfillment.util.Merger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("PMD.TooManyMethods")
public abstract class BaseCommunicationService<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseCommunicationService.class);

  protected RestOperations restTemplate = new RestTemplate();

  @Autowired
  protected AuthService authService;

  @Value("${request.maxUrlLength}")
  private int maxUrlLength;
  
  protected abstract String getServiceUrl();

  protected abstract String getUrl();

  protected abstract Class<T> getResultClass();

  protected abstract Class<T[]> getArrayResultClass();

  protected URI buildUri(String url) {
    return createUri(url);
  }

  public void setRestTemplate(RestOperations template) {
    this.restTemplate = template;
  }

  /**
   * Return all reference data T objects.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @return all reference data T objects.
   */
  protected Collection<T> findAll(String resourceUrl, RequestParameters parameters) {
    return findAllWithMethod(resourceUrl, parameters, null, HttpMethod.GET);
  }

  protected Collection<T> findAllWithMethod(String resourceUrl,
      RequestParameters uriParameters, Map<String, Object> payload, HttpMethod method) {
    String url = getServiceUrl() + getUrl() + resourceUrl;

    try {
      ResponseEntity<T[]> responseEntity = runWithTokenRetry(
          () -> doListRequest(url, uriParameters, payload, method, getArrayResultClass())
      );

      return new ArrayList<>(Arrays.asList(responseEntity.getBody()));
    } catch (HttpStatusCodeException ex) {
      throw buildDataRetrievalException(ex);
    }
  }

  protected <P> ServiceResponse<List<P>> tryFindAll(String resourceUrl, Class<P[]> type,
                                                    String etag) {
    String url = getServiceUrl() + getUrl() + resourceUrl;

    try {
      RequestHeaders headers = RequestHeaders.init().setIfNoneMatch(etag);
      ResponseEntity<P[]> response = restTemplate.exchange(
          url, HttpMethod.GET, RequestHelper.createEntity(null, addAuthHeader(headers)), type
      );

      if (response.getStatusCode() == HttpStatus.NOT_MODIFIED) {
        return new ServiceResponse<>(null, response.getHeaders(), false);
      } else {
        List<P> list = Stream.of(response.getBody()).collect(Collectors.toList());
        return new ServiceResponse<>(list, response.getHeaders(), true);
      }
    } catch (HttpStatusCodeException ex) {
      throw buildDataRetrievalException(ex);
    }
  }

  /**
   * Return all reference data T objects for Page that need to be retrieved with GET request.
   *
   * @param parameters  Map of query parameters.
   * @return Page of reference data T objects.
   */
  protected Page<T> getPage(RequestParameters parameters) {
    return getPage("", parameters, null, HttpMethod.GET, getResultClass());
  }

  /**
   * Return all reference data T objects for Page that need to be retrieved with POST request.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @param payload     body to include with the outgoing request.
   * @return Page of reference data T objects.
   */
  protected Page<T> getPage(String resourceUrl, RequestParameters parameters, Object payload) {
    return getPage(resourceUrl, parameters, payload, HttpMethod.POST, getResultClass());
  }

  protected <P> Page<P> getPage(String resourceUrl, RequestParameters parameters, Object payload,
                                HttpMethod method, Class<P> type) {
    String url = getServiceUrl() + getUrl() + resourceUrl;

    try {
      ResponseEntity<PageDto<P>> response = runWithTokenRetry(
          () -> doPageRequest(url, parameters, payload, method, type)
      );
      return response.getBody();

    } catch (HttpStatusCodeException ex) {
      throw buildDataRetrievalException(ex);
    }
  }

  private <E> ResponseEntity<E[]> doListRequest(String url, RequestParameters parameters,
      Object payload, HttpMethod method,
      Class<E[]> type) {
    HttpEntity<Object> entity = RequestHelper
        .createEntity(payload, RequestHeaders.init().setAuth(authService.obtainAccessToken()));
    List<E[]> arrays = new ArrayList<>();

    for (URI uri : RequestHelper.splitRequest(url, parameters, maxUrlLength)) {
      arrays.add(restTemplate.exchange(uri, method, entity, type).getBody());
    }

    E[] body = Merger
        .ofArrays(arrays)
        .withDefaultValue(() -> (E[]) Array.newInstance(type.getComponentType(), 0))
        .merge();

    return new ResponseEntity<>(body, HttpStatus.OK);
  }

  private <E> ResponseEntity<PageDto<E>> doPageRequest(String url,
      RequestParameters parameters,
      Object payload,
      HttpMethod method,
      Class<E> type) {

    LOGGER.debug("fulfillment page request url: {}, parameters: {}, method: {}",
        url, parameters, method);

    HttpEntity<Object> entity = RequestHelper
        .createEntity(payload, RequestHeaders.init().setAuth(authService.obtainAccessToken()));
    ParameterizedTypeReference<PageDto<E>> parameterizedType =
        new DynamicPageTypeReference<>(type);
    List<PageDto<E>> pages = new ArrayList<>();

    for (URI uri : RequestHelper.splitRequest(url, parameters, maxUrlLength)) {
      pages.add(restTemplate.exchange(uri, method, entity, parameterizedType).getBody());
    }

    PageDto<E> body = Merger
        .ofPages(pages)
        .withDefaultValue(PageDto::new)
        .merge();

    return new ResponseEntity<>(body, HttpStatus.OK);
  }

  protected <P> ResponseEntity<P> runWithTokenRetry(HttpTask<P> task) {
    try {
      return task.run();
    } catch (HttpStatusCodeException ex) {
      if (HttpStatus.UNAUTHORIZED == ex.getStatusCode()) {
        // the token has (most likely) expired - clear the cache and retry once
        authService.clearTokenCache();
        return task.run();
      }
      throw ex;
    }
  }

  @FunctionalInterface
  protected interface HttpTask<T> {

    ResponseEntity<T> run();

  }

  protected DataRetrievalException buildDataRetrievalException(HttpStatusCodeException ex) {
    return new DataRetrievalException(getResultClass().getSimpleName(), ex);
  }

  protected <E> HttpEntity<E> createEntity(E payload) {
    if (payload == null) {
      return createEntity();
    } else {
      return RequestHelper.createEntity(payload, createHeadersWithAuth());
    }
  }

  protected  <E> HttpEntity<E> createEntity() {
    return RequestHelper.createEntity(createHeadersWithAuth());
  }

  private RequestHeaders addAuthHeader(RequestHeaders headers) {
    return null == headers
        ? RequestHeaders.init().setAuth(authService.obtainAccessToken())
        : headers.setAuth(authService.obtainAccessToken());
  }

  private RequestHeaders createHeadersWithAuth() {
    return RequestHeaders.init().setAuth(authService.obtainAccessToken());
  }
}
