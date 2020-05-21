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

import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.MutablePair;
import org.openlmis.referencedata.dto.RightAssignmentDto;
import org.openlmis.referencedata.util.Resource2Db;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

/**
 * RightAssignmentInitializer runs after its associated Spring application has loaded. It 
 * automatically re-generates right assignments into the database, after dropping the existing 
 * right assignments. This component only runs when the "refresh-db" Spring profile is set.
 */
@Service
public class RightAssignmentService {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(RightAssignmentService.class);

  private static final String USER_ID = "userid";
  private static final String RIGHT_NAME = "rightname";
  private static final String RIGHT_ASSIGNMENTS_PATH = "classpath:db/right-assignments/";

  static final String DELETE_SQL = "DELETE FROM referencedata.right_assignments;";

  @Value(value = RIGHT_ASSIGNMENTS_PATH + "get_right_assignments.sql")
  private Resource rightAssignmentsResource;

  @Value(value = RIGHT_ASSIGNMENTS_PATH + "get_all_supervised_facilities_from_node.sql")
  private Resource supervisedFacilitiesResource;

  @Autowired
  private JdbcTemplate template;

  /**
   * Re-generates right assignments. This operation needs to be transactional so that dropping 
   * and re-generating is one transaction. The isolation level is specified to READ_COMMITTED, 
   * to allow proper reads on the right assignments table. This is so that any permission checks 
   * do not have to wait for this re-generation to finish, but can use the "old" right 
   * assignments. This is acceptable since the right assignments table is not expected to change 
   * very often, and the re-generation could take several seconds to finish.
   */
  @Async("rightAssignmentTaskExecutor")
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Future<Void> regenerateRightAssignments() {
    Profiler profiler = new Profiler("REGENERATE_RIGHT_ASSIGNMENTS");
    profiler.setLogger(XLOGGER);
    XLOGGER.entry();

    // Drop existing rows; we are regenerating from scratch
    profiler.start("DROP_RIGHT_ASSIGNMENTS");
    template.update(DELETE_SQL);

    // Get a right assignment matrix from database
    profiler.start("GET_INTERMEDIATE_RIGHT_ASSIGNMENTS");
    List<RightAssignmentDto> dbRightAssignments = new ArrayList<>();
    try {
      dbRightAssignments = getRightAssignmentsFromDbResource(rightAssignmentsResource);
    } catch (IOException ioe) {
      XLOGGER.warn("Error when getting right assignments: " + ioe.getMessage());
    }

    profiler.start("RESOURCE_2_DB");
    Resource2Db r2db = new Resource2Db(template);
    try {
      profiler.start("CHANGE_SUPERVISORY_NODES_TO_FACILITIES_IN_RIGHT_ASSIGNMENTS");
      Set<RightAssignmentDto> rightAssignmentsToInsert = convertForInsert(dbRightAssignments,
          supervisedFacilitiesResource);

      profiler.start("INSERT_INTO_DB");
      for (List partialRightAssignments : Iterables.partition(rightAssignmentsToInsert, 100)) {
        insertFromDbRightAssignmentList(r2db, partialRightAssignments);
      }
    } catch (IOException ioe) {
      XLOGGER.warn("Error when getting inserting right assignments: " + ioe.getMessage());
    }

    XLOGGER.exit();
    profiler.stop().log();
    return new AsyncResult<>(null);
  }

  private void insertFromDbRightAssignmentList(Resource2Db resource2Db,
      List<RightAssignmentDto> rightAssignmentDtos) {
    // Convert set of right assignments to insert to a set of SQL inserts
    XLOGGER.debug("Convert right assignments to SQL inserts");
    MutablePair dataWithHeader = new MutablePair<List<String>, List<Object[]>>();
    dataWithHeader.setRight(rightAssignmentDtos.stream()
        .map(rad -> rad.toColumnArray())
        .collect(Collectors.toList()));

    // set column headers
    dataWithHeader.setLeft(Arrays.asList("id",
        USER_ID,
        RIGHT_NAME,
        "facilityid",
        "programid"));

    // insert into right_assignments
    XLOGGER.debug("Perform SQL inserts");
    resource2Db.insertToDbFromBatchedPair("referencedata.right_assignments", dataWithHeader);
  }

  List<RightAssignmentDto> getRightAssignmentsFromDbResource(Resource resource)
      throws IOException {
    return template.query(
        resourceToString(resource),
        (ResultSet rs, int rowNum) -> {

          RightAssignmentDto rightAssignmentMap = new RightAssignmentDto();
          rightAssignmentMap.setUserId(UUID.fromString(rs.getString(USER_ID)));
          rightAssignmentMap.setRightName(rs.getString(RIGHT_NAME));
          if (null != rs.getString("facilityid")) {
            rightAssignmentMap.setFacilityId(UUID.fromString(rs.getString("facilityid")));
          }
          if (null != rs.getString("programid")) {
            rightAssignmentMap.setProgramId(UUID.fromString(rs.getString("programid")));
          }
          if (null != rs.getString("supervisorynodeid")) {
            rightAssignmentMap.setSupervisoryNodeId(
                UUID.fromString(rs.getString("supervisorynodeid")));
          }
          return rightAssignmentMap;
        }
    );
  }

  Set<RightAssignmentDto> convertForInsert(List<RightAssignmentDto> rightAssignments,
      Resource supervisedFacilitiesResource)
      throws IOException {
    Set<RightAssignmentDto> rightAssignmentsToInsert = new HashSet<>();
    for (RightAssignmentDto rightAssignment : rightAssignments) {

      if (null != rightAssignment.getSupervisoryNodeId()) {

        // Special case: supervisory node is present. We need to expand the supervisory node and 
        // turn it into a list of all facility IDs being supervised by this node.

        // Get all supervised facilities. Add each facility to the set.
        List<UUID> facilityIds = getSupervisedFacilityIds(supervisedFacilitiesResource,
            rightAssignment.getSupervisoryNodeId(),
            rightAssignment.getProgramId());

        for (UUID facilityId : facilityIds) {

          rightAssignmentsToInsert.add(new RightAssignmentDto(
              rightAssignment.getUserId(),
              rightAssignment.getRightName(),
              facilityId,
              rightAssignment.getProgramId()));
        }
      } else {

        // All other cases: home facility supervision, fulfillment and direct right assignments.
        // Just copy everything but the supervisoryNodeId and add it to the set.
        rightAssignmentsToInsert.add(new RightAssignmentDto(
            rightAssignment.getUserId(),
            rightAssignment.getRightName(),
            rightAssignment.getFacilityId(),
            rightAssignment.getProgramId()));
      }
    }

    return rightAssignmentsToInsert;
  }

  private List<UUID> getSupervisedFacilityIds(Resource supervisedFacilitiesResource,
      UUID supervisoryNodeId, UUID programId)
      throws IOException {

    return template.queryForList(
        resourceToString(supervisedFacilitiesResource),
        UUID.class,
        supervisoryNodeId,
        programId);
  }

  private String resourceToString(final Resource resource) throws IOException {
    XLOGGER.entry(resource.getDescription());
    String str;
    try (InputStream is = resource.getInputStream()) {
      str = StreamUtils.copyToString(is, Charset.defaultCharset());
    }
    XLOGGER.exit();
    return str;
  }
}