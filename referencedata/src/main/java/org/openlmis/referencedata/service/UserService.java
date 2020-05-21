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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;
import org.openlmis.referencedata.util.messagekeys.RightMessageKeys;
import org.openlmis.referencedata.util.messagekeys.SupervisoryNodeMessageKeys;
import org.openlmis.referencedata.util.messagekeys.UserMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  protected static final String USERNAME = "username";
  protected static final String FIRST_NAME = "firstName";
  protected static final String LAST_NAME = "lastName";
  protected static final String EMAIL = "email";
  protected static final String HOME_FACILITY_ID = "homeFacilityId";
  protected static final String ACTIVE = "active";
  protected static final String VERIFIED = "verified";
  protected static final String EXTRA_DATA = "extraData";

  private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

  @Autowired
  private UserRepository userRepository;
  
  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private RightRepository rightRepository;

  @Autowired
  private ProgramRepository programRepository;

  @Autowired
  private SupervisoryNodeRepository supervisoryNodeRepository;
  
  private ObjectMapper mapper = new ObjectMapper();

  /**
   * Method returns users with matched parameters or all users if params are empty.
   *
   * @param searchParams {@link UserSearchParams}.
   *                     There can be multiple id params, other params are ignored if id is
   *                     provided. When id is not provided and if other params have multiple values,
   *                     the first one is used. Empty params are allowed. {@code not null}.
   * @param pageable pagination parameters.
   * @return Page of users. All users will be returned when map is null or empty.
   */
  public Page<User> searchUsersById(UserSearchParams searchParams, Pageable pageable) {
    if (searchParams.isEmpty()) {
      return userRepository.findAll(pageable);
    }

    return searchUsers(searchParams, pageable);
  }

  /**
   * Method returns all users with matched parameters.
   *
   * @param searchParams request parameters (username, firstName, lastName, email, homeFacility,
   *                 active, verified) and JSON extraData.
   * @param pageable pagination parameters
   * @return Page of users
   */
  public Page<User> searchUsers(UserSearchParams searchParams, Pageable pageable) {

    Profiler profiler = new Profiler("SERVICE_USER_SEARCH");
    profiler.setLogger(LOGGER);

    profiler.start("GET_EXTRA_DATA_FROM_PARAMS");
    Map<String, String> extraData = searchParams.getExtraData();

    profiler.start("SEARCHING_BY_EXTRA_DATA");
    List<User> foundUsers = null;
    if (extraData != null && !extraData.isEmpty()) {
      try {
        profiler.start("SEARCHING_BY_EXTRA_DATA_IN_REPOSITORY");
        String extraDataString = mapper.writeValueAsString(extraData);
        foundUsers = userRepository.findByExtraData(extraDataString);
        if (foundUsers.isEmpty()) {
          return Pagination.getPage(foundUsers, pageable, 0);
        }
      } catch (JsonProcessingException jpe) {
        LOGGER.error("Cannot serialize extra data query request body into JSON", jpe);
      }
    }

    profiler.start("SEARCH_IN_DB");
    Page<User> result = userRepository.searchUsers(searchParams, foundUsers, pageable);

    profiler.stop().log();

    return result;
  }

  /**
   * Searches for users based having the specified right. The params are required
   * based on the type of the right.
   * @param rightId the ID of the right, always required
   * @param programId the ID of the program, required for supervision rights
   * @param supervisoryNodeId the ID of the supervisory node,
   *                          required for supervision rights
   * @param warehouseId the ID of the warehouse, required for fulfillment rights
   * @return users with the right assigned, matching the criteria
   */
  public Set<User> rightSearch(UUID rightId, UUID programId, UUID supervisoryNodeId,
                               UUID warehouseId) {

    Right right = rightRepository.findOne(rightId);

    if (right == null) {
      throw new ValidationMessageException(RightMessageKeys.ERROR_NOT_FOUND);
    }

    if (right.getType() == RightType.ORDER_FULFILLMENT) {
      return searchByFulfillmentRight(right, warehouseId);
    } else if (right.getType() == RightType.SUPERVISION) {
      return searchBySupervisionRight(rightId, supervisoryNodeId, programId);
    } else {
      return userRepository.findUsersByDirectRight(right);
    }
  }

  private Set<User> searchByFulfillmentRight(Right right, UUID warehouseId) {
    if (warehouseId == null) {
      throw new ValidationMessageException(UserMessageKeys.WAREHOUSE_ID_REQUIRED);
    }

    Facility warehouse = facilityRepository.findOne(warehouseId);

    if (warehouse == null) {
      throw new ValidationMessageException(new Message(
          FacilityMessageKeys.ERROR_NOT_FOUND_WITH_ID, warehouseId));
    }

    return userRepository.findUsersByFulfillmentRight(right, warehouse);
  }

  private Set<User> searchBySupervisionRight(UUID rightId, UUID supervisoryNodeId,
      UUID programId) {
    if (programId == null) {
      throw new ValidationMessageException(UserMessageKeys.PROGRAM_ID_REQUIRED);
    }

    if (!programRepository.exists(programId)) {
      throw new ValidationMessageException(new Message(
          ProgramMessageKeys.ERROR_NOT_FOUND_WITH_ID, programId));
    }

    if (null == supervisoryNodeId) {
      return userRepository.findUsersBySupervisionRight(rightId, programId);
    }

    if (!supervisoryNodeRepository.exists(supervisoryNodeId)) {
      throw new ValidationMessageException(new Message(
          SupervisoryNodeMessageKeys.ERROR_NOT_FOUND, supervisoryNodeId));
    }

    return userRepository.findUsersBySupervisionRight(rightId, supervisoryNodeId, programId);
  }

}
