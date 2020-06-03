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

package org.openlmis.fulfillment.service.stockmanagement;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.fulfillment.service.BaseCommunicationServiceTest;
import org.openlmis.fulfillment.web.stockmanagement.ValidSourceDestinationDto;
import org.openlmis.fulfillment.web.stockmanagement.ValidSourceDestinationDtoDataBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

public abstract class ValidSourceDestinationsStockManagementServiceTest
    extends BaseCommunicationServiceTest<ValidSourceDestinationDto> {

  private ValidSourceDestinationsStockManagementService service;

  protected abstract String getUrl();

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    service = (ValidSourceDestinationsStockManagementService) prepareService();
    ReflectionTestUtils.setField(service, "stockmanagementUrl", "http://localhost");
  }

  @Override
  protected ValidSourceDestinationDto generateInstance() {
    return new ValidSourceDestinationDto();
  }

  @Test
  public void shouldFindOne() {
    // given
    UUID facility = UUID.randomUUID();
    ValidSourceDestinationDto destination = new ValidSourceDestinationDtoDataBuilder()
        .withNode(facility)
        .build();
    ResponseEntity<ValidSourceDestinationDto[]> response = new ResponseEntity<>(
        new ValidSourceDestinationDto[]{destination}, HttpStatus.OK
    );

    when(restTemplate
        .exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(service.getArrayResultClass())))
        .thenReturn(response);

    // when
    UUID program = UUID.randomUUID();
    UUID fromFacilityId = UUID.randomUUID();
    Optional<ValidSourceDestinationDto> validSourceDestination = service
        .search(program, fromFacilityId, facility);

    // then
    assertThat(validSourceDestination.isPresent(), is(true));
    assertThat(validSourceDestination.get(), is(destination));

    verify(restTemplate)
        .exchange(uriCaptor.capture(), eq(HttpMethod.GET), entityCaptor.capture(),
            eq(service.getArrayResultClass()));

    String uri = uriCaptor.getValue().toString();
    String url = service.getServiceUrl() + getUrl();

    assertThat(
        uri,
        allOf(
            startsWith(url),
            containsString("programId=" + program),
            containsString("facilityId=" + fromFacilityId)
        )
    );

    HttpEntity<String> entity = entityCaptor.getValue();
    assertAuthHeader(entity);
    assertThat(entity.getBody(), is(nullValue()));
  }
}
