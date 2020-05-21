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

package org.openlmis.referencedata.web;

import static java.util.stream.Collectors.toSet;
import static org.openlmis.referencedata.util.messagekeys.SystemMessageKeys.ERROR_INVALID_FORMAT_DATE;
import static org.openlmis.referencedata.util.messagekeys.SystemMessageKeys.ERROR_INVALID_FORMAT_UUID;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections4.MapUtils;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.UuidUtil;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@NoArgsConstructor
@EqualsAndHashCode
@ToString
public final class SearchParams {

  private static final String PAGE = "page";
  private static final String SIZE = "size";
  private static final String SORT = "sort";
  private static final String ACCESS_TOKEN = "access_token";

  private MultiValueMap<String, Object> params;

  /**
   * Constructs new SearchParams object from {@code MultiValueMap}.
   */
  public SearchParams(MultiValueMap<String, Object> queryMap) {
    if (queryMap != null) {
      params = new LinkedMultiValueMap<>(queryMap);
      params.remove(PAGE);
      params.remove(SIZE);
      params.remove(SORT);
      params.remove(ACCESS_TOKEN);
    } else {
      params = new LinkedMultiValueMap<>();
    }
  }

  public boolean containsKey(String key) {
    return params.containsKey(key);
  }

  public String getFirst(String key) {
    return (String) params.getFirst(key);
  }

  public Collection<Object> get(String key) {
    return params.get(key);
  }

  public Map getMap(String key) {
    return (Map) params.getFirst(key);
  }

  public LinkedMultiValueMap<String, Object> asMultiValueMap() {
    return new LinkedMultiValueMap<>(params);
  }

  public Collection<String> keySet() {
    return params.keySet();
  }

  public boolean isEmpty() {
    return MapUtils.isEmpty(params);
  }

  /**
   * Parses String value into {@link LocalDate}.
   * If format is wrong {@link ValidationMessageException} will be thrown.
   *
   * @param key key for value be parsed into LocalDate
   * @return parsed local date
   */
  public LocalDate getLocalDate(String key) {
    String value = getFirst(key);

    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException cause) {
      throw new ValidationMessageException(cause,
          new Message(ERROR_INVALID_FORMAT_DATE, value, key));
    }
  }

  /**
   * Parses String value into {@link Boolean}.
   * If format is wrong null value will be returned.
   *
   * @param key key for value be parsed into LocalDate
   * @return parsed local date
   */
  public Boolean getBoolean(String key) {
    return Boolean.valueOf(params.getFirst(key).toString());
  }

  /**
   * Parses String value into {@link UUID} based on given key.
   * If format is wrong {@link ValidationMessageException} will be thrown.
   *
   * @param key key for value be parsed into UUID
   * @return parsed UUID
   */
  public UUID getUuid(String key) {
    if (!containsKey(key)) {
      return null;
    }
    return parse(getFirst(key), key);
  }

  /**
   * Parses String value into {@link UUID} based on given key.
   * If format is wrong {@link ValidationMessageException} will be thrown.
   *
   * @param key key for value be parsed into UUID
   * @return parsed list of UUIDs
   */
  public Set<UUID> getUuids(String key) {
    return Optional
        .ofNullable(get(key))
        .orElse(Collections.emptyList())
        .stream()
        .map(value -> parse((String) value, key))
        .collect(toSet());
  }

  /**
   * Gets set of {@link String} based on given key.
   *
   * @param key key for getting string values
   * @return parsed set of strings
   */
  public Set<String> getStrings(String key) {
    return Optional.ofNullable(get(key))
        .orElse(Collections.emptyList())
        .stream()
        .map(value -> (String) value)
        .collect(toSet());
  }

  private UUID parse(String value, String key) {
    return UuidUtil.fromString(value)
        .orElseThrow(() ->
            new ValidationMessageException(new Message(ERROR_INVALID_FORMAT_UUID, value, key)));
  }
}
