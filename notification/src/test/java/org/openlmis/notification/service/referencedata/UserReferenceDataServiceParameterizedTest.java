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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockitoAnnotations;
import org.openlmis.notification.service.ResultDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@RunWith(Parameterized.class)
public class UserReferenceDataServiceParameterizedTest
    extends BaseReferenceDataServiceTest<UserDto> {

  private static final String URI_QUERY_NAME = "name";
  private static final String URI_QUERY_VALUE = "value";

  @Override
  protected BaseReferenceDataService<UserDto> getService() {
    return new UserReferenceDataService();
  }

  @Override
  protected UserDto generateInstance() {
    return new UserDto();
  }

  @Override
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    super.setUp();
  }

  private UUID user = UUID.randomUUID();
  private UUID right = UUID.randomUUID();
  private UUID program;
  private UUID facility;
  private UUID warehouse;

  /**
   * Creates new instance of Parameterized Test.
   *
   * @param program   UUID of program
   * @param facility  UUID of facility
   * @param warehouse UUID of facility
   */
  public UserReferenceDataServiceParameterizedTest(UUID program, UUID facility, UUID warehouse) {
    this.program = program;
    this.facility = facility;
    this.warehouse = warehouse;
  }

  /**
   * Get test data.
   *
   * @return collection of objects that will be passed to test constructor.
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {null, null, null},
        {null, null, UUID.randomUUID()},
        {null, UUID.randomUUID(), null},
        {UUID.randomUUID(), null, null},
        {null, UUID.randomUUID(), UUID.randomUUID()},
        {UUID.randomUUID(), null, UUID.randomUUID()},
        {UUID.randomUUID(), UUID.randomUUID(), null},
        {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()}
    });
  }

  @Test
  public void shouldCheckUserRight() {
    executeHasRightEndpoint(user, right, program, facility, warehouse, true);
    executeHasRightEndpoint(user, right, program, facility, warehouse, false);
  }

  private void executeHasRightEndpoint(UUID user, UUID right, UUID program, UUID facility,
                                       UUID warehouse, boolean expectedValue) {
    // given
    UserReferenceDataService service = (UserReferenceDataService) prepareService();
    ResponseEntity<ResultDto> response = mock(ResponseEntity.class);

    // when
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET),
         any(HttpEntity.class), eq(ResultDto.class)))
        .thenReturn(response);
    when(response.getBody()).thenReturn(new ResultDto<>(expectedValue));

    ResultDto result = service.hasRight(user, right, program, facility, warehouse);

    // then
    assertThat(result.getResult(), is(expectedValue));

    verify(restTemplate, atLeastOnce()).exchange(
        uriCaptor.capture(), eq(HttpMethod.GET), entityCaptor.capture(), eq(ResultDto.class)
    );

    URI uri = uriCaptor.getValue();
    List<NameValuePair> parse = URLEncodedUtils.parse(uri, "UTF-8");

    assertThat(parse, hasItem(allOf(
        hasProperty(URI_QUERY_NAME, is("rightId")),
        hasProperty(URI_QUERY_VALUE, is(right.toString())))
    ));

    assertAuthHeader(entityCaptor.getValue());
    assertThat(entityCaptor.getValue().getBody(), is(nullValue()));

    if (null != program) {
      assertThat(parse, hasItem(allOf(
          hasProperty(URI_QUERY_NAME, is("programId")),
          hasProperty(URI_QUERY_VALUE, is(program.toString())))
      ));
    } else {
      assertThat(parse, not(hasItem(hasProperty(URI_QUERY_NAME, is("programId")))));
    }

    if (null != facility) {
      assertThat(parse, hasItem(allOf(
          hasProperty(URI_QUERY_NAME, is("facilityId")),
          hasProperty(URI_QUERY_VALUE, is(facility.toString())))
      ));
    } else {
      assertThat(parse, not(hasItem(hasProperty(URI_QUERY_NAME, is("facilityId")))));
    }

    if (null != warehouse) {
      assertThat(parse, hasItem(allOf(
          hasProperty(URI_QUERY_NAME, is("warehouseId")),
          hasProperty(URI_QUERY_VALUE, is(warehouse.toString())))
      ));
    } else {
      assertThat(parse, not(hasItem(hasProperty(URI_QUERY_NAME, is("warehouseId")))));
    }
  }

}
