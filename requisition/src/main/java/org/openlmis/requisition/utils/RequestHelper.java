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

package org.openlmis.requisition.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.openlmis.requisition.exception.EncodingException;
import org.openlmis.requisition.service.RequestHeaders;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

public final class RequestHelper {

  private RequestHelper() {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a {@link URI} from the given string representation without any parameters.
   */
  public static URI createUri(String url) {
    return createUri(url, null);
  }

  /**
   * Creates a {@link URI} from the given string representation and with the given parameters.
   */
  public static URI createUri(String url, RequestParameters parameters) {
    UriComponentsBuilder builder = UriComponentsBuilder.newInstance().uri(URI.create(url));

    RequestParameters
        .init()
        .setAll(parameters)
        .forEach(e -> e.getValue().forEach(one -> {
          try {
            builder.queryParam(e.getKey(),
                UriUtils.encodeQueryParam(String.valueOf(one),
                    StandardCharsets.UTF_8.name()));
          } catch (UnsupportedEncodingException ex) {
            throw new EncodingException(ex);
          }
        }));

    return builder.build(true).toUri();
  }

  /**
   * Creates an {@link HttpEntity} with the given payload as a body and adds an authorization
   * header with the provided token.
   * @param payload the body of the request, pass null if no body
   * @param token the token to put into the authorization header
   * @param <E> the type of the body for the request
   * @return the {@link HttpEntity} to use
   */
  public static <E> HttpEntity<E> createEntity(E payload, String token) {
    if (payload == null) {
      return createEntity(token);
    } else {
      return new HttpEntity<>(payload, createHeadersWithAuth(token));
    }
  }

  public static <E> HttpEntity<E> createEntity(String token) {
    return new HttpEntity<>(createHeadersWithAuth(token));
  }

  /**
   * Creates an {@link HttpEntity} with the given payload as a body and headers.
   */
  public static <E> HttpEntity<E> createEntity(E payload, RequestHeaders headers) {
    return new HttpEntity<>(payload, headers.toHeaders());
  }

  /**
   * Creates an {@link HttpEntity} with the given headers.
   */
  public static <E> HttpEntity<E> createEntity(RequestHeaders headers) {
    return new HttpEntity<>(headers.toHeaders());
  }

  /**
   * Split the given {@link RequestParameters} into smaller chunks.
   */
  public static URI[] splitRequest(String url, RequestParameters queryParams, int maxUrlLength) {
    RequestParameters safeQueryParams = RequestParameters.init().setAll(queryParams);
    URI uri = createUri(url, safeQueryParams);

    if (uri.toString().length() > maxUrlLength) {
      Pair<RequestParameters, RequestParameters> split = safeQueryParams.split();

      if (null != split.getLeft() && null != split.getRight()) {
        URI[] left = splitRequest(url, split.getLeft(), maxUrlLength);
        URI[] right = splitRequest(url, split.getRight(), maxUrlLength);

        return Stream
            .concat(Arrays.stream(left), Arrays.stream(right))
            .distinct()
            .toArray(URI[]::new);
      }
    }

    return new URI[]{uri};
  }

  private static HttpHeaders createHeadersWithAuth(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    return headers;
  }

}
