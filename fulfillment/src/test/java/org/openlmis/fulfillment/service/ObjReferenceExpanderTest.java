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

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_DTO_EXPANSION_ASSIGNMENT;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_DTO_EXPANSION_CAST;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_DTO_EXPANSION_HREF;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.fulfillment.testutils.ExpandedObjectReferenceDto;
import org.openlmis.fulfillment.testutils.TestDto;
import org.openlmis.fulfillment.testutils.TestDtoDataBuilder;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("PMD.UnusedPrivateField")
public class ObjReferenceExpanderTest {

  private static final String EXPANDED_STRING_VALUE = "property1";
  private static final List<String> EXPANDED_LIST_VALUE = Arrays.asList("element1", "element2");
  private static final UUID EXPANDED_UUID_VALUE = UUID.randomUUID();
  private static final ExpandedObjectReferenceDto EXPANDED_NESTED_PROPERTY =
      new ExpandedObjectReferenceDto();
  private static final String EXPANDED_OBJECT_REFERENCE_DTO_FIELD = "expandedObjectReferenceDto";
  private static final String EXPANDED_NESTED_PROPERTY_FIELD = "expandedNestedProperty";
  private static final String EXPANDED_NESTED_FIELD =
      EXPANDED_OBJECT_REFERENCE_DTO_FIELD + '.' + EXPANDED_NESTED_PROPERTY_FIELD;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Mock
  private AuthService authService;

  @Mock
  private RestTemplate restTemplate;

  @InjectMocks
  private ObjReferenceExpander objReferenceExpander = new ObjReferenceExpander();

  private TestDto testDto;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    objReferenceExpander.registerConverters(); // This is normally called by Spring's @PostConstruct
    testDto = new TestDtoDataBuilder().buildDtoWithObjectReferenceNotExpanded();
  }

  @Test
  public void shouldThrowExceptionIfExpandedFieldIsNotObjectReferenceDto() {
    expected.expect(ValidationException.class);
    expected.expectMessage(ERROR_DTO_EXPANSION_CAST);

    objReferenceExpander.expandDto(testDto, singleton("uuidProperty"));
  }

  @Test
  public void shouldThrowExceptionIfExpandedFieldDoesNotExist() {
    expected.expect(ValidationException.class);
    expected.expectMessage(ERROR_DTO_EXPANSION_ASSIGNMENT);

    objReferenceExpander.expandDto(testDto, singleton("nonExistingField"));
  }

  @Test
  public void shouldThrowExceptionIfExpandedFieldDoesNotHaveHrefPropertySet() {
    expected.expect(ValidationException.class);
    expected.expectMessage(ERROR_DTO_EXPANSION_HREF);

    testDto = new TestDtoDataBuilder().buildDtoWithEmptyObjectReference();
    objReferenceExpander.expandDto(testDto, singleton(EXPANDED_OBJECT_REFERENCE_DTO_FIELD));
  }

  @Test
  public void shouldNotFailIfResourceDoesNotExist() {
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(RequestEntity.class),
        eq(Map.class))).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

    objReferenceExpander.expandDto(testDto, singleton(EXPANDED_OBJECT_REFERENCE_DTO_FIELD));

    assertNotNull(testDto);
    ExpandedObjectReferenceDto actual = testDto.getExpandedObjectReferenceDto();
    checkOriginalProperties(actual);

    // No expanded properties should be set
    assertNull(actual.getExpandedStringProperty());
    assertNull(actual.getExpandedListProperty());
    assertNull(actual.getExpandedUuidProperty());
    assertNull(actual.getExpandedNestedProperty());
  }

  @Test
  public void shouldExpandDto() {
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("expandedStringProperty", EXPANDED_STRING_VALUE);
    responseMap.put("expandedListProperty", EXPANDED_LIST_VALUE);
    responseMap.put("expandedUuidProperty", EXPANDED_UUID_VALUE);
    responseMap.put("expandedNestedProperty", EXPANDED_NESTED_PROPERTY);

    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(RequestEntity.class),
        eq(Map.class))).thenReturn(ResponseEntity.ok(responseMap));

    objReferenceExpander.expandDto(testDto, singleton(EXPANDED_OBJECT_REFERENCE_DTO_FIELD));

    ExpandedObjectReferenceDto actual = testDto.getExpandedObjectReferenceDto();
    checkOriginalProperties(actual);

    assertNotNull(actual.getExpandedStringProperty());
    assertEquals(EXPANDED_STRING_VALUE, actual.getExpandedStringProperty());

    assertNotNull(actual.getExpandedListProperty());
    assertEquals(2, actual.getExpandedListProperty().size());
    assertEquals(EXPANDED_LIST_VALUE, actual.getExpandedListProperty());

    assertNotNull(actual.getExpandedUuidProperty());
    assertEquals(EXPANDED_UUID_VALUE, actual.getExpandedUuidProperty());

    assertNotNull(actual.getExpandedNestedProperty());
    assertEquals(EXPANDED_NESTED_PROPERTY, actual.getExpandedNestedProperty());
  }

  @Test
  public void shouldHandleNestedExpand() {
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("expandedStringProperty", EXPANDED_STRING_VALUE);
    responseMap.put("expandedListProperty", EXPANDED_LIST_VALUE);
    responseMap.put("expandedUuidProperty", EXPANDED_UUID_VALUE);
    responseMap.put("expandedNestedProperty", EXPANDED_NESTED_PROPERTY);

    ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);

    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(RequestEntity.class),
        eq(Map.class))).thenReturn(ResponseEntity.ok(responseMap));

    objReferenceExpander.expandDto(testDto, singleton(EXPANDED_NESTED_FIELD));

    verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(RequestEntity.class),
        eq(Map.class));

    URI value = uriCaptor.getValue();
    assertThat(value.toString(), endsWith("?expand=" + EXPANDED_NESTED_PROPERTY_FIELD));
  }

  private void checkOriginalProperties(ObjectReferenceDto actual) {
    assertNotNull(actual);
    // Original properties of the DTO should not be lost
    assertNotNull(actual.getHref());
    assertNotNull(actual.getId());
  }
}
