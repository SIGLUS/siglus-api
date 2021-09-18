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

package org.siglus.siglusapi.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.stockmanagement.service.RequestHeaders;
import org.openlmis.stockmanagement.util.DynamicPageTypeReference;
import org.openlmis.stockmanagement.util.Merger;
import org.openlmis.stockmanagement.util.PageImplRepresentation;
import org.openlmis.stockmanagement.util.RequestHelper;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.util.Message;
import org.siglus.siglusapi.exception.DataRetrievalException;
import org.siglus.siglusapi.service.AuthService;
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

@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public abstract class BaseCommunicationService<T> {

  @Autowired
  protected AuthService authService;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${request.maxUrlLength}")
  private int maxUrlLength;

  protected RestOperations restTemplate = new RestTemplate();

  protected abstract String getServiceUrl();

  protected abstract String getUrl();

  protected abstract Class<T> getResultClass();

  protected abstract Class<T[]> getArrayResultClass();

  /**
   * Return one object from service.
   *
   * @param id UUID of requesting object.
   * @return Requesting reference data object.
   */
  public T findOne(UUID id) {
    return findOne(id.toString(), RequestParameters.init());
  }

  public T findOne(UUID id, boolean obtainUserToken) {
    return findOne(id.toString(), RequestParameters.init(), obtainUserToken);
  }

  /**
   * Return one object from service.
   *
   * @param parameters Map of query parameters.
   * @return Requesting reference data object.
   */
  public T findOne(RequestParameters parameters) {
    return findOne(null, parameters, getResultClass());
  }

  /**
   * Return one object from service.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @return one reference data T objects.
   */
  public T findOne(String resourceUrl, RequestParameters parameters) {
    return findOne(resourceUrl, parameters, getResultClass());
  }

  public T findOne(String resourceUrl, RequestParameters parameters, boolean obtainUserToken) {
    return findOne(resourceUrl, parameters, getResultClass(), obtainUserToken);
  }

  /**
   * Return one object from service.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @param type        set to what type a response should be converted.
   * @return one reference data T objects.
   */
  public T findOne(String resourceUrl, RequestParameters parameters, Class<T> type) {
    return findOne(resourceUrl, parameters, type, Boolean.FALSE);
  }

  public T findOne(String resourceUrl, RequestParameters parameters, Class<T> type,
      boolean obtainUserToken) {
    String url = getServiceUrl() + getUrl() + StringUtils.defaultIfBlank(resourceUrl, "");

    RequestParameters params = RequestParameters
        .init()
        .setAll(parameters);

    try {
      return runWithTokenRetry(() -> restTemplate.exchange(
          RequestHelper.createUri(url, params),
          HttpMethod.GET,
          createEntity(null, obtainUserToken),
          type)).getBody();
    } catch (HttpStatusCodeException ex) {
      // rest template will handle 404 as an exception, instead of returning null
      if (HttpStatus.NOT_FOUND == ex.getStatusCode()) {
        log.warn("{} matching params does not exist. Params: {}",
            getResultClass().getSimpleName(), parameters);
        return null;
      } else {
        throw buildDataRetrievalException(ex);
      }
    }
  }

  public Collection<T> findAll() {
    return findAll("");
  }

  public Collection<T> findAll(String resourceUrl) {
    return findAll(resourceUrl, getArrayResultClass());
  }

  /**
   * Return all reference data T objects.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @return all reference data T objects.
   */
  protected Collection<T> findAll(String resourceUrl, Map<String, Object> parameters) {
    return findAll(resourceUrl, parameters, Boolean.FALSE, getArrayResultClass());
  }

  public <P> Collection<P> findAll(String resourceUrl, Class<P[]> type) {
    return findAll(resourceUrl, Collections.<String, Object>emptyMap(), false, type);
  }

  protected Collection<T> findAll(String resourceUrl, Map<String, Object> parameters,
      boolean obtainUserToken) {
    return findAll(resourceUrl, parameters, obtainUserToken, getArrayResultClass());
  }

  protected <P> Collection<P> findAll(String resourceUrl, Map<String, Object> parameters,
      boolean obtainUserToken, Class<P[]> type) {
    String url = getServiceUrl() + getUrl() + resourceUrl;

    RequestParameters params = RequestParameters.of(parameters);

    try {
      ResponseEntity<P[]> responseEntity = runWithTokenRetry(
          () -> doListRequest(url, params, HttpMethod.GET, type, obtainUserToken)
      );
      return new ArrayList<>(Arrays.asList(responseEntity.getBody()));
    } catch (HttpStatusCodeException ex) {
      throw buildDataRetrievalException(ex);
    }
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

  /**
   * Return all reference data T objects for Page that need to be retrieved with POST request.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @param payload     body to include with the outgoing request.
   * @return Page of reference data T objects.
   */
  public Page<T> getPage(String resourceUrl, Map<String, Object> parameters, Object payload) {
    return getPage(resourceUrl, parameters, payload, HttpMethod.POST, getResultClass());
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

  /**
   * Return all reference data T objects for Page that need to be retrieved with GET request.
   *
   * @param parameters  Map of query parameters.
   * @return Page of reference data T objects.
   */
  public Page<T> getPage(RequestParameters parameters) {
    return getPage("", parameters, null, HttpMethod.GET, getResultClass());
  }

  /**
   * Return all reference data T objects for Page that need to be retrieved with GET request.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @return Page of reference data T objects.
   */
  public Page<T> getPage(String resourceUrl, RequestParameters parameters) {
    return getPage(resourceUrl, parameters, null, HttpMethod.GET, getResultClass());
  }

  protected Page<T> getPage(Map<String, Object> parameters) {
    return getPage("", parameters);
  }

  protected Page<T> getPage(String resourceUrl, Map<String, Object> parameters) {
    return getPage(resourceUrl, parameters, null, HttpMethod.GET, getResultClass());
  }

  protected <P> Page<P> getPage(String resourceUrl, Map<String, Object> parameters, Object payload,
      HttpMethod method, Class<P> type) {
    RequestParameters params = RequestParameters.init();
    parameters.forEach(params::set);

    return getPage(resourceUrl, params, payload, method, type);
  }

  protected <P> Page<P> getPage(String resourceUrl, RequestParameters parameters, Object payload,
      HttpMethod method, Class<P> type) {
    return getPage(resourceUrl, parameters, payload, method, type, Boolean.FALSE);
  }

  protected <P> Page<P> getPage(String resourceUrl, RequestParameters parameters, Object payload,
      HttpMethod method, Class<P> type, boolean obtainUserToken) {
    String url = getServiceUrl() + getUrl() + resourceUrl;

    try {
      ResponseEntity<PageImplRepresentation<P>> response = runWithTokenRetry(
          () -> doPageRequest(url, parameters, payload, method, type, obtainUserToken)
      );
      return response.getBody();

    } catch (HttpStatusCodeException ex) {
      throw buildDataRetrievalException(ex);
    }
  }

  protected <P, R> R postResult(String resourceUrl, P payload, Class<R> type) {
    return postResult(resourceUrl, payload, type, Boolean.FALSE);
  }

  protected <P, R> R postResult(String resourceUrl, P payload, Class<R> type,
      boolean obtainUserToken) {
    String url = getServiceUrl() + getUrl() + resourceUrl;
    R response;
    try {
      response = restTemplate.postForObject(
              RequestHelper.createUri(url),
              RequestHelper.createEntity(payload, authService.obtainAccessToken(obtainUserToken)),
              type);
    } catch (HttpStatusCodeException ex) {
      final Message errorMessage = Message.createFromMessageKeyStr(ex.getResponseBodyAsString());
      throw new ValidationMessageException(ex, errorMessage);
    }

    return response;
  }

  protected <P, R> R put(String resourceUrl, P payload, Class<R> type, boolean obtainUserToken) {
    String url = getServiceUrl() + getUrl() + resourceUrl;
    R response;
    try {
      response = restTemplate.exchange(RequestHelper.createUri(url), HttpMethod.PUT,
          createEntity(payload, obtainUserToken), type).getBody();
    } catch (HttpStatusCodeException ex) {
      final Message errorMessage = Message.createFromMessageKeyStr(ex.getResponseBodyAsString());
      throw new ValidationMessageException(ex, errorMessage);
    }
    return response;
  }

  protected void delete(String resourceUrl, boolean obtainUserToken) {
    String url = getServiceUrl() + getUrl() + resourceUrl;
    try {
      restTemplate
          .exchange(RequestHelper.createUri(url), HttpMethod.DELETE,
              createEntity(null, obtainUserToken), Void.class);
    } catch (HttpStatusCodeException ex) {
      final Message errorMessage = Message.createFromMessageKeyStr(ex.getResponseBodyAsString());
      throw new ValidationMessageException(ex, errorMessage);
    }
  }

  protected <K, V> Map<K, V> getMap(String resourceUrl, RequestParameters parameters,
                                    Class<K> keyType, Class<V> valueType) {
    String url = getServiceUrl() + getUrl() + StringUtils.defaultIfBlank(resourceUrl, "");
    TypeFactory factory = objectMapper.getTypeFactory();
    MapType mapType = factory.constructMapType(HashMap.class, keyType, valueType);

    HttpEntity<Object> entity = createEntity();
    List<Map<K, V>> maps = new ArrayList<>();

    for (URI uri : RequestHelper.splitRequest(url, parameters, maxUrlLength)) {
      ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
      Map<K, V> map = objectMapper.convertValue(response.getBody(), mapType);
      maps.add(map);
    }

    return Merger
        .ofMaps(maps)
        .withDefaultValue(Collections::emptyMap)
        .merge();
  }

  private <E> ResponseEntity<E[]> doListRequest(String url, RequestParameters parameters,
                                                HttpMethod method, Class<E[]> type,
                                                boolean obtainUserToken) {
    HttpEntity<Object> entity = createEntity(null, obtainUserToken);
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

  private <E> ResponseEntity<PageImplRepresentation<E>> doPageRequest(String url,
                                                                      RequestParameters parameters,
                                                                      Object payload,
                                                                      HttpMethod method,
                                                                      Class<E> type,
                                                                      boolean obtainUserToken) {
    HttpEntity<Object> entity = createEntity(payload, obtainUserToken);
    ParameterizedTypeReference<PageImplRepresentation<E>> parameterizedType =
        new DynamicPageTypeReference<>(type);
    List<PageImplRepresentation<E>> pages = new ArrayList<>();

    for (URI uri : RequestHelper.splitRequest(url, parameters, maxUrlLength)) {
      pages.add(restTemplate.exchange(uri, method, entity, parameterizedType).getBody());
    }

    PageImplRepresentation<E> body = Merger
        .ofPages(pages)
        .withDefaultValue(PageImplRepresentation::new)
        .merge();

    return new ResponseEntity<>(body, HttpStatus.OK);
  }

  protected DataRetrievalException buildDataRetrievalException(HttpStatusCodeException ex) {
    return new DataRetrievalException(getResultClass().getSimpleName(), ex);
  }

  protected <P> ResponseEntity<P> runWithRetryAndTokenRetry(HttpTask<P> task) {
    try {
      return task.run();
    } catch (HttpStatusCodeException ex) {
      if (HttpStatus.UNAUTHORIZED == ex.getStatusCode()) {
        // the token has (most likely) expired - clear the cache and retry once
        authService.clearTokenCache();
        return runWithRetry(task);
      }
      if (ex.getStatusCode().is4xxClientError() || ex.getStatusCode().is5xxServerError()) {
        return runWithTokenRetry(task);
      }
      throw ex;
    }
  }

  private <P> ResponseEntity<P> runWithRetry(HttpTask<P> task) {
    try {
      return task.run();
    } catch (HttpStatusCodeException ex) {
      if (ex.getStatusCode().is4xxClientError() || ex.getStatusCode().is5xxServerError()) {
        return task.run();
      }
      throw ex;
    }
  }

  private <E> HttpEntity<E> createEntity(E payload, boolean obtainUserToken) {
    if (payload == null) {
      return RequestHelper.createEntity(createHeadersWithAuth(obtainUserToken));
    } else {
      return RequestHelper.createEntity(payload, createHeadersWithAuth(obtainUserToken));
    }
  }

  private <E> HttpEntity<E> createEntity() {
    return RequestHelper.createEntity(createHeadersWithAuth());
  }

  private RequestHeaders createHeadersWithAuth() {
    return createHeadersWithAuth(Boolean.FALSE);
  }

  private RequestHeaders createHeadersWithAuth(boolean obtainUserToken) {
    return RequestHeaders.init().setAuth(authService.obtainAccessToken(obtainUserToken));
  }

  @FunctionalInterface
  protected interface HttpTask<T> {

    ResponseEntity<T> run();

  }

}
