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

package org.openlmis.referencedata.service;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.RightType;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.repository.RightRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.repository.UserRepository;
import org.openlmis.referencedata.repository.UserSearchParams;
import org.openlmis.referencedata.testbuilder.UserDataBuilder;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.UserSearchParamsDataBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@SuppressWarnings("PMD.TooManyMethods")
public class UserServiceTest {

  private static final String EXTRA_DATA_VALUE = "extraDataValue";
  private static final String FIRST_NAME_SEARCH = "FirstNameMatchesTwoUsers";
  private static final String EXTRA_DATA_KEY = "extraDataKey";

  private static final UUID RIGHT_ID = UUID.randomUUID();
  private static final UUID WAREHOUSE_ID = UUID.randomUUID();
  private static final UUID SUPERVISORY_NODE_ID = UUID.randomUUID();
  private static final UUID PROGRAM_ID = UUID.randomUUID();

  @Mock
  private UserRepository userRepository;

  @Mock
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Mock
  private ProgramRepository programRepository;

  @Mock
  private FacilityRepository facilityRepository;

  @Mock
  private RightRepository rightRepository;

  @Mock
  private Facility warehouse;

  @Mock
  private Right right;

  @Mock
  private Pageable pageable;

  @InjectMocks
  private UserService userService;

  private User user;
  private User user2;

  private UserSearchParams userSearch;
  private Map<String, String> extraData;
  private ObjectMapper mapper = new ObjectMapper();
  private String extraDataString;

  @Before
  public void setUp() throws JsonProcessingException {
    MockitoAnnotations.initMocks(this);
    user = generateUser();
    user2 = mock(User.class);
    userSearch = new UserSearchParams(FIRST_NAME_SEARCH);
    extraData = Collections.singletonMap(EXTRA_DATA_KEY, EXTRA_DATA_VALUE);
    extraDataString = mapper.writeValueAsString(extraData);
    when(pageable.getPageSize()).thenReturn(10);
    when(pageable.getPageNumber()).thenReturn(1);
  }

  @Test
  public void searchUsersShouldUseExtraDataString() {
    when(userRepository
        .searchUsers(any(UserSearchParams.class), any(List.class), any(Pageable.class)))
        .thenReturn(Pagination.getPage(Arrays.asList(user, user2), null, 2));

    userSearch.setExtraData(extraData);
    when(userRepository.findByExtraData(any(String.class))).thenReturn(Arrays.asList(user, user2));

    Page<User> receivedUsers = userService.searchUsers(userSearch, pageable);

    assertEquals(2, receivedUsers.getContent().size());
    verify(userRepository).findByExtraData(extraDataString);
    verify(userRepository).searchUsers(userSearch, Arrays.asList(user, user2), pageable);
  }

  @Test
  public void searchUsersShouldReturnEmptyPageIfExtraDataSearchReturnedNoResults() {
    when(userRepository
        .searchUsers(any(UserSearchParams.class), any(List.class), any(Pageable.class)))
        .thenReturn(Pagination.getPage(Arrays.asList(user, user2), null, 2));

    userSearch.setExtraData(extraData);

    when(userRepository.findByExtraData(any(String.class))).thenReturn(Collections.emptyList());

    Page<User> receivedUsers = userService.searchUsers(userSearch, pageable);

    assertEquals(0, receivedUsers.getContent().size());
    verify(userRepository).findByExtraData(extraDataString);
    verify(userRepository, never())
        .searchUsers(any(UserSearchParams.class), any(List.class), any(Pageable.class));
  }

  @Test
  public void searchUsersShouldNotSearchExtraDataIfParameterIsNullOrEmpty() {
    when(userRepository
        .searchUsers(any(UserSearchParams.class), any(List.class), any(Pageable.class)))
        .thenReturn(Pagination.getPage(Arrays.asList(user, user2), null, 2));

    Page<User> receivedUsers = userService.searchUsers(userSearch, pageable);

    assertEquals(2, receivedUsers.getContent().size());
    assertTrue(receivedUsers.getContent().contains(user));
    assertTrue(receivedUsers.getContent().contains(user2));
    verify(userRepository, never()).findByExtraData(any(String.class));
    verify(userRepository).searchUsers(userSearch, null, pageable);
  }

  @Test
  public void searchUsersShouldSearchByAllParameters() {
    when(userRepository
        .searchUsers(any(UserSearchParams.class), any(List.class), any(Pageable.class)))
        .thenReturn(Pagination.getPage(Arrays.asList(user, user2), null, 2));

    UserSearchParams searchParams = new UserSearchParamsDataBuilder()
        .withFirstName(FIRST_NAME_SEARCH)
        .withExtraData(extraData)
        .build();

    Facility homeFacility = new Facility("some-code");
    when(facilityRepository.findOne(searchParams.getHomeFacilityUuid()))
        .thenReturn(homeFacility);

    List<User> foundUsers = Arrays.asList(user, user2);
    when(userRepository.findByExtraData(any(String.class))).thenReturn(foundUsers);

    Page<User> receivedUsers = userService.searchUsers(searchParams, pageable);

    assertEquals(2, receivedUsers.getContent().size());
    assertTrue(receivedUsers.getContent().contains(user));
    assertTrue(receivedUsers.getContent().contains(user2));
    verify(userRepository).searchUsers(searchParams, foundUsers, pageable);
  }

  @Test
  public void rightSearchShouldFindByDirectRightAssignments() {
    Set<User> expected = newHashSet(user, user2);
    when(rightRepository.findOne(RIGHT_ID)).thenReturn(right);
    when(right.getType()).thenReturn(RightType.GENERAL_ADMIN);
    when(userRepository.findUsersByDirectRight(right))
        .thenReturn(expected);

    Set<User> users = userService.rightSearch(RIGHT_ID, null, null, null);

    assertEquals(expected, users);
    verify(userRepository).findUsersByDirectRight(right);
  }

  @Test
  public void rightSearchShouldFindByFulfillmentAssignment() {
    Set<User> expected = newHashSet(user, user2);
    when(rightRepository.findOne(RIGHT_ID)).thenReturn(right);
    when(right.getType()).thenReturn(RightType.ORDER_FULFILLMENT);
    when(facilityRepository.findOne(WAREHOUSE_ID)).thenReturn(warehouse);
    when(warehouse.isWarehouse()).thenReturn(true);
    when(userRepository.findUsersByFulfillmentRight(right, warehouse))
        .thenReturn(expected);

    Set<User> users = userService.rightSearch(RIGHT_ID, null, null, WAREHOUSE_ID);

    assertEquals(expected, users);
    verify(userRepository).findUsersByFulfillmentRight(right, warehouse);
  }

  @Test
  public void rightSearchShouldFindBySupervisionAssignment() {
    Set<User> expected = newHashSet(user, user2);
    when(rightRepository.findOne(RIGHT_ID)).thenReturn(right);
    when(right.getType()).thenReturn(RightType.SUPERVISION);
    when(supervisoryNodeRepository.exists(SUPERVISORY_NODE_ID)).thenReturn(true);
    when(programRepository.exists(PROGRAM_ID)).thenReturn(true);
    when(userRepository.findUsersBySupervisionRight(RIGHT_ID, SUPERVISORY_NODE_ID, PROGRAM_ID))
        .thenReturn(expected);

    Set<User> users = userService.rightSearch(RIGHT_ID, PROGRAM_ID,
        SUPERVISORY_NODE_ID, null);

    assertEquals(expected, users);
    verify(userRepository).findUsersBySupervisionRight(RIGHT_ID, SUPERVISORY_NODE_ID, PROGRAM_ID);
  }

  @Test
  public void rightSearchShouldFindBySupervisionAssignmentWithoutSupervisoryNode() {
    Set<User> expected = newHashSet(user, user2);
    when(rightRepository.findOne(RIGHT_ID)).thenReturn(right);
    when(right.getType()).thenReturn(RightType.SUPERVISION);
    when(programRepository.exists(PROGRAM_ID)).thenReturn(true);
    when(userRepository.findUsersBySupervisionRight(RIGHT_ID, PROGRAM_ID))
        .thenReturn(expected);

    Set<User> users = userService.rightSearch(RIGHT_ID, PROGRAM_ID, null, null);

    assertEquals(expected, users);
    verify(userRepository).findUsersBySupervisionRight(RIGHT_ID, PROGRAM_ID);
  }

  @Test(expected = ValidationMessageException.class)
  public void rightSearchShouldRequireExistingRight() {
    when(rightRepository.findOne(RIGHT_ID)).thenReturn(null);

    try {
      userService.rightSearch(RIGHT_ID, PROGRAM_ID, SUPERVISORY_NODE_ID, null);
    } finally {
      verifyZeroInteractions(userRepository, facilityRepository,
          supervisoryNodeRepository, programRepository);
    }
  }

  @Test(expected = ValidationMessageException.class)
  public void rightSearchShouldRequireWarehouseIdForFulfillmentRights() {
    when(rightRepository.findOne(RIGHT_ID)).thenReturn(right);
    when(right.getType()).thenReturn(RightType.ORDER_FULFILLMENT);

    try {
      userService.rightSearch(RIGHT_ID, null, null, null);
    } finally {
      verifyZeroInteractions(userRepository, supervisoryNodeRepository,
          facilityRepository, programRepository);
    }
  }

  @Test(expected = ValidationMessageException.class)
  public void rightSearchShouldThrowExceptionForNonExistentFacility() {
    when(rightRepository.findOne(RIGHT_ID)).thenReturn(right);
    when(right.getType()).thenReturn(RightType.ORDER_FULFILLMENT);
    when(facilityRepository.findOne(WAREHOUSE_ID)).thenReturn(null);

    try {
      userService.rightSearch(RIGHT_ID, null, null, WAREHOUSE_ID);
    } finally {
      verify(facilityRepository).findOne(WAREHOUSE_ID);
      verifyZeroInteractions(supervisoryNodeRepository, programRepository,
          userRepository);
    }
  }

  @Test
  public void rightSearchShouldNotThrowExceptionForNonWarehouseFacility() {
    when(rightRepository.findOne(RIGHT_ID)).thenReturn(right);
    when(right.getType()).thenReturn(RightType.ORDER_FULFILLMENT);
    when(facilityRepository.findOne(WAREHOUSE_ID)).thenReturn(warehouse);
    when(warehouse.isWarehouse()).thenReturn(false);

    userService.rightSearch(RIGHT_ID, null, null, WAREHOUSE_ID);

    verify(userRepository).findUsersByFulfillmentRight(right, warehouse);
  }

  @Test(expected = ValidationMessageException.class)
  public void rightSearchShouldRequireProgramIdForSupervisoryRights() {
    when(rightRepository.findOne(RIGHT_ID)).thenReturn(right);
    when(right.getType()).thenReturn(RightType.SUPERVISION);

    try {
      userService.rightSearch(RIGHT_ID, null, SUPERVISORY_NODE_ID, null);
    } finally {
      verifyZeroInteractions(userRepository, supervisoryNodeRepository,
          programRepository, facilityRepository);
    }
  }

  @Test(expected = ValidationMessageException.class)
  public void rightSearchShouldThrowExceptionForNonExistentProgram() {
    when(rightRepository.findOne(RIGHT_ID)).thenReturn(right);
    when(right.getType()).thenReturn(RightType.SUPERVISION);
    when(supervisoryNodeRepository.exists(SUPERVISORY_NODE_ID)).thenReturn(true);
    when(programRepository.exists(PROGRAM_ID)).thenReturn(false);

    try {
      userService.rightSearch(RIGHT_ID, PROGRAM_ID, SUPERVISORY_NODE_ID, null);
    } finally {
      verify(programRepository).exists(PROGRAM_ID);
      verifyZeroInteractions(facilityRepository, userRepository);
    }
  }

  @Test(expected = ValidationMessageException.class)
  public void rightSearchShouldThrowExceptionForNonExistentSupervisoryNode() {
    when(rightRepository.findOne(RIGHT_ID)).thenReturn(right);
    when(right.getType()).thenReturn(RightType.SUPERVISION);
    when(supervisoryNodeRepository.exists(SUPERVISORY_NODE_ID)).thenReturn(false);
    when(programRepository.exists(PROGRAM_ID)).thenReturn(true);

    try {
      userService.rightSearch(RIGHT_ID, PROGRAM_ID, SUPERVISORY_NODE_ID, null);
    } finally {
      verify(supervisoryNodeRepository).exists(SUPERVISORY_NODE_ID);
      verifyZeroInteractions(facilityRepository, userRepository);
    }
  }

  private User generateUser() {
    return new UserDataBuilder()
        .withHomeFacilityId(UUID.randomUUID())
        .withExtraData(EXTRA_DATA_KEY, EXTRA_DATA_VALUE)
        .build();
  }
}
