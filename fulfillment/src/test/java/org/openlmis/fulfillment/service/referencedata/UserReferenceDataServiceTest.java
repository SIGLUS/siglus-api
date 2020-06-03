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

package org.openlmis.fulfillment.service.referencedata;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.fulfillment.service.PageDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class UserReferenceDataServiceTest extends BaseReferenceDataServiceTest<UserDto> {

  UserReferenceDataService service;

  @Override
  protected BaseReferenceDataService<UserDto> getService() {
    return new UserReferenceDataService();
  }

  @Override
  protected UserDto generateInstance() {
    return new UserDto();
  }

  @Before
  public void before() {
    service = (UserReferenceDataService) prepareService();
  }

  @Test
  public void shouldFindUserByName() {
    // given
    String name = "userName";

    UserDto userDto = generateInstance();
    userDto.setUsername(name);

    Map<String, Object> payload = new HashMap<>();
    payload.put("username", name);

    ResponseEntity response = mock(ResponseEntity.class);

    // when
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(response);

    PageDto<UserDto> page = new PageDto<>(new PageImpl<>(ImmutableList.of(userDto)));

    when(response.getBody()).thenReturn(page);

    UserDto user = service.findUser(name);

    // then
    verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST),
            entityCaptor.capture(), any(ParameterizedTypeReference.class));

    URI uri = uriCaptor.getValue();
    String url = service.getServiceUrl() + service.getUrl() + "search";

    assertThat(uri.toString(), is(equalTo(url)));
    assertThat(user.getUsername(), is(equalTo(name)));

    assertAuthHeader(entityCaptor.getValue());
    assertThat(entityCaptor.getValue().getBody(), is(payload));
  }

  @Test
  public void shouldReturnNullIfUserCannotBeFound() {
    // given
    String name = "userName";

    ResponseEntity response = mock(ResponseEntity.class);

    Map<String, Object> payload = new HashMap<>();
    payload.put("username", name);

    // when
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
            .thenReturn(response);

    PageDto<UserDto> page = new PageDto<>(new PageImpl<>(Collections.emptyList()));
    when(response.getBody()).thenReturn(page);

    UserDto user = service.findUser(name);

    // then
    verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST),
            entityCaptor.capture(), any(ParameterizedTypeReference.class));

    URI uri = uriCaptor.getValue();
    String url = service.getServiceUrl() + service.getUrl() + "search";

    assertThat(uri.toString(), is(equalTo(url)));
    assertThat(user, is(nullValue()));

    assertAuthHeader(entityCaptor.getValue());
    assertThat(entityCaptor.getValue().getBody(), is(payload));
  }

  @Test
  public void shouldFindUsersByIds() {
    // given
    UUID id = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    List<UUID> ids = Arrays.asList(id, id2);

    UserDto user = generateInstance();
    user.setId(id);
    UserDto anotherUser = generateInstance();
    anotherUser.setId(id2);

    Map<String, Object> payload = new HashMap<>();
    payload.put("id", ids);
    ResponseEntity response = mock(ResponseEntity.class);

    // when
    when(response.getBody()).thenReturn(
        new PageDto<>(new PageImpl<>(Arrays.asList(user, anotherUser)))
    );
    when(restTemplate.exchange(
        any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
        any(ParameterizedTypeReference.class)
    )).thenReturn(response);

    List<UserDto> users = service.findByIds(ids);

    // then
    verify(restTemplate).exchange(
        uriCaptor.capture(), eq(HttpMethod.GET),
        entityCaptor.capture(), any(ParameterizedTypeReference.class)
    );
    assertTrue(users.contains(user));
    assertTrue(users.contains(anotherUser));

    String actualUrl = uriCaptor.getValue().toString();
    assertTrue(actualUrl.startsWith(service.getServiceUrl() + service.getUrl()));
    assertTrue(actualUrl.contains(id.toString()));
    assertTrue(actualUrl.contains(id2.toString()));

    assertAuthHeader(entityCaptor.getValue());
  }

  @Test
  public void shouldReturnEmptyListWhenFindingUsersWithNoIdsProvided() {
    // given
    checkAuth = false;
    // when
    List<UserDto> users = service.findByIds(Collections.emptyList());
    // then
    assertThat(users, empty());
  }

  /*@Test
  public void shouldRetrievePermissionStrings() throws Exception {
    UserDto instance = generateInstance();
    String etag = RandomStringUtils.random(10);

    // when
    mockArrayRequest(HttpMethod.GET, String[].class);
    mockArrayResponse(response ->
        when(response.getBody()).thenReturn(new String[]{"PERMISSION_STRING"}));

    ServiceResponse<List<String>> found = service.getPermissionStrings(instance.getId(), etag);

    // then
    ResponseEntity response = getArrayResponse();

    assertThat(found.getBody(), hasItem("PERMISSION_STRING"));
    assertThat(found.getHeaders(), equalTo(response.getHeaders()));
    assertThat(found.isModified(), is(true));

    URI uri = getUri();
    String url = service.getServiceUrl() + service.getUrl() + "/permissionStrings";
    assertThat(uri.toString(), equalTo(url));

    HttpEntity entity = getEntity();

    assertThat(entity.getBody(), is(nullValue()));
    assertThat(entity.getHeaders(), hasEntry(HttpHeaders.IF_NONE_MATCH, singletonList(etag)));
  }

  @Test
  public void shouldNotRetrievePermissionStringsIfThereWasNoChange() throws Exception {
    UserDto instance = generateInstance();
    String etag = RandomStringUtils.random(10);

    // when
    mockArrayRequest(HttpMethod.GET, String[].class);
    mockArrayResponse(response ->
        when(response.getStatusCode()).thenReturn(HttpStatus.NOT_MODIFIED));

    ServiceResponse<List<String>> found = service.getPermissionStrings(instance.getId(), etag);

    // then
    ResponseEntity response = getArrayResponse();

    assertThat(found.getBody(), is(nullValue()));
    assertThat(found.getHeaders(), equalTo(response.getHeaders()));
    assertThat(found.isModified(), is(false));

    URI uri = getUri();
    String url = service.getServiceUrl() + service.getUrl() + "/permissionStrings";
    assertThat(uri.toString(), equalTo(url));

    HttpEntity entity = getEntity();

    assertThat(entity.getBody(), is(nullValue()));
    assertThat(entity.getHeaders(), hasEntry(HttpHeaders.IF_NONE_MATCH, singletonList(etag)));
  }*/
}
