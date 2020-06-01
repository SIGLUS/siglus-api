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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.Collection;
import org.junit.Test;
import org.openlmis.notification.service.BaseCommunicationService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class TogglzReferenceDataServiceTest
    extends BaseReferenceDataServiceTest<TogglzFeatureDto> {

  @Override
  protected TogglzFeatureDto generateInstance() {
    return new TogglzFeatureDto();
  }

  @Override
  protected BaseCommunicationService<TogglzFeatureDto> getService() {
    return new TogglzReferenceDataService();
  }

  @Test
  public void shouldFindAllResources() {
    // given
    TogglzReferenceDataService service = (TogglzReferenceDataService) prepareService();
    TogglzFeatureDto dto = generateInstance();
    ResponseEntity<TogglzFeatureDto[]> response = mock(ResponseEntity.class);

    given(response.getBody()).willReturn(new TogglzFeatureDto[]{dto});
    given(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET),
        any(HttpEntity.class), eq(service.getArrayResultClass())))
        .willReturn(response);

    // when
    Collection<TogglzFeatureDto> found = service.findAll();

    // then
    assertThat(found, hasItem(dto));

    verify(restTemplate).exchange(
        uriCaptor.capture(), eq(HttpMethod.GET),
        entityCaptor.capture(), eq(service.getArrayResultClass())
    );

    URI uri = uriCaptor.getValue();
    String url = service.getServiceUrl() + service.getUrl();

    assertThat(uri.toString(), is(url));

    assertAuthHeader(entityCaptor.getValue());
    assertThat(entityCaptor.getValue().getBody(), is(nullValue()));
  }

}
