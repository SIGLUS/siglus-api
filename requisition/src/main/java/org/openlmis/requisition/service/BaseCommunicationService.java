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

package org.openlmis.requisition.service;

import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SERVICE_OCCURED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SERVICE_REQUIRED;
import static org.openlmis.requisition.utils.RequestHelper.createEntity;
import static org.openlmis.requisition.utils.RequestHelper.createUri;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openlmis.requisition.dto.ResultDto;
import org.openlmis.requisition.utils.DynamicPageTypeReference;
import org.openlmis.requisition.utils.DynamicResultDtoTypeReference;
import org.openlmis.requisition.utils.Merger;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.utils.RequestHelper;
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
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected RestOperations restTemplate = new RestTemplate();

  protected AuthService authService;

  @Value("${request.maxUrlLength}")
  private int maxUrlLength;

  protected abstract String getServiceUrl();

  protected abstract String getUrl();

  protected abstract Class<T> getResultClass();

  protected abstract Class<T[]> getArrayResultClass();

  protected abstract String getServiceName();

  /**
   * Return one object from service.
   *
   * @param id UUID of requesting object.
   * @return Requesting reference data object.
   */
  public T findOne(UUID id) {
    return findOne(id.toString(), RequestParameters.init());
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

  /**
   * Return one object from service.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @param type        set to what type a response should be converted.
   * @return one reference data T objects.
   */
  public <P> P findOne(String resourceUrl, RequestParameters parameters, Class<P> type) {
    String url = getServiceUrl() + getUrl() + resourceUrl;

    RequestParameters params = RequestParameters
        .init()
        .setAll(parameters);

    try {
      return runWithTokenRetry(() -> restTemplate.exchange(
              createUri(url, params),
              HttpMethod.GET,
              createEntity(authService.obtainAccessToken()),
              type)).getBody();
    } catch (HttpStatusCodeException ex) {
      // rest template will handle 404 as an exception, instead of returning null
      if (HttpStatus.NOT_FOUND == ex.getStatusCode()) {
        logger.warn(
            "{} matching params does not exist. Params: {}",
            getResultClass().getSimpleName(), parameters
        );

        return null;
      }

      throw buildDataRetrievalException(ex);
    }
  }

  public List<T> findAll() {
    return findAll("");
  }

  public List<T> findAll(String resourceUrl) {
    return findAll(resourceUrl, getArrayResultClass());
  }

  public <P> List<P> findAll(String resourceUrl, Class<P[]> type) {
    return findAll(resourceUrl, RequestParameters.init(), type);
  }

  /**
   * Return all reference data T objects.
   *
   * @param resourceUrl Endpoint url.
   * @param parameters  Map of query parameters.
   * @return all reference data T objects.
   */
  public List<T> findAll(String resourceUrl, RequestParameters parameters) {
    return findAll(resourceUrl, parameters, getArrayResultClass());
  }

  public <P> List<P> findAll(String resourceUrl, RequestParameters parameters, Class<P[]> type) {
    return findAll(resourceUrl, parameters, null, HttpMethod.GET, type);
  }

  protected <P> List<P> findAll(String resourceUrl, RequestParameters parameters,
                          Object payload, HttpMethod method, Class<P[]> type) {
    String url = getServiceUrl() + getUrl() + resourceUrl;

    RequestParameters params = RequestParameters
        .init()
        .setAll(parameters);

    try {
      ResponseEntity<P[]> response = runWithTokenRetry(
          () -> doListRequest(url, params, payload, method, type)
      );

      return Stream.of(response.getBody()).collect(Collectors.toList());
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

  public Page<T> getPage(RequestParameters parameters) {
    return getPage("", parameters);
  }

  protected Page<T> getPage(String resourceUrl, RequestParameters parameters) {
    return getPage(resourceUrl, parameters, null, HttpMethod.GET, getResultClass());
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
    RequestParameters params = RequestParameters
        .init()
        .setAll(parameters);

    try {
      ResponseEntity<PageDto<P>> response = runWithTokenRetry(
          () -> doPageRequest(url, params, payload, method, type)
      );
      return response.getBody();
    } catch (HttpStatusCodeException ex) {
      throw buildDataRetrievalException(ex);
    }
  }

  protected <P> ResultDto<P> getResult(String resourceUrl, RequestParameters parameters,
                                       Class<P> type) {
    String url = getServiceUrl() + getUrl() + resourceUrl;
    RequestParameters params = RequestParameters
        .init()
        .setAll(parameters);

    ResponseEntity<ResultDto<P>> response = runWithTokenRetry(() -> restTemplate.exchange(
        createUri(url, params),
        HttpMethod.GET,
        createEntity(authService.obtainAccessToken()),
        new DynamicResultDtoTypeReference<>(type)
    ));

    return response.getBody();
  }

  private <E> ResponseEntity<E[]> doListRequest(String url, RequestParameters parameters,
                                                Object payload, HttpMethod method,
                                                Class<E[]> type) {
    HttpEntity<Object> entity = RequestHelper
        .createEntity(payload, authService.obtainAccessToken());
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
    HttpEntity<Object> entity = RequestHelper
        .createEntity(payload, authService.obtainAccessToken());
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

  @FunctionalInterface
  protected interface HttpTask<T> {

    ResponseEntity<T> run();

  }

  protected DataRetrievalException buildDataRetrievalException(HttpStatusCodeException ex) {
    String errorKey;
    if (ex.getStatusCode().is5xxServerError() || ex.getStatusCode() == HttpStatus.NOT_FOUND) {
      errorKey = ERROR_SERVICE_REQUIRED;
    } else {
      errorKey = ERROR_SERVICE_OCCURED;
    }
    return new DataRetrievalException(
        new Message(errorKey, getServiceName()),
        getResultClass().getSimpleName(),
        ex.getStatusCode(),
        ex.getResponseBodyAsString());
  }

  @Autowired
  public void setAuthService(AuthService authService) {
    this.authService = authService;
  }

  void setRestTemplate(RestOperations template) {
    this.restTemplate = template;
  }

  private RequestHeaders addAuthHeader(RequestHeaders headers) {
    return null == headers
        ? RequestHeaders.init().setAuth(authService.obtainAccessToken())
        : headers.setAuth(authService.obtainAccessToken());
  }
}
