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

package org.openlmis.referencedata.validate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.dto.FacilityDto;
import org.openlmis.referencedata.dto.RequisitionGroupBaseDto;
import org.openlmis.referencedata.dto.SupervisoryNodeBaseDto;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.repository.RequisitionGroupRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.util.messagekeys.RequisitionGroupMessageKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * A validator for {@link RequisitionGroupBaseDto} object.
 */
@Component
public class RequisitionGroupValidator implements BaseValidator {

  // RequisitionGroup fields
  static final String CODE = "code";
  static final String NAME = "name";
  static final String DESCRIPTION = "description";
  static final String SUPERVISORY_NODE = "supervisoryNode";
  static final String MEMBER_FACILITIES = "memberFacilities";
  static final String REQUISITION_GROUP_PROGRAM_SCHEDULES = "requisitionGroupProgramSchedules";

  @Autowired
  private SupervisoryNodeRepository supervisoryNodes;

  @Autowired
  private RequisitionGroupRepository requisitionGroups;

  @Autowired
  private FacilityRepository facilities;

  @Autowired
  private ProgramRepository programs;

  /**
   * Checks if the given class definition is supported.
   *
   * @param clazz the {@link Class} that this {@link Validator} is being asked if it can {@link
   *              #validate(Object, Errors) validate}
   * @return true if {@code clazz} is equal to {@link RequisitionGroupBaseDto} class definition.
   *     Otherwise false.
   */
  @Override
  public boolean supports(Class<?> clazz) {
    return RequisitionGroupBaseDto.class.equals(clazz);
  }

  /**
   * Validates the {@code target} object, which must be an instance of
   * {@link RequisitionGroupBaseDto} class.
   * <p>Firstly, the method checks if the target object has a value in {@code code} and {@code name}
   * properties. For those two properties the value cannot be {@code null}, empty or
   * contains only whitespaces.</p>
   * <p>Secondly, the method checks if the target object has a non-null value in
   * {@code supervisoryNode}.</p>
   * <p>If there are no errors, the method checks if the {@code code} property in the target
   * object is unique and if the Supervisory Node exists in database.</p>
   *
   * @param target the object that is to be validated (never {@code null})
   * @param errors contextual state about the validation process (never {@code null})
   * @throws NullPointerException if any method parameter is null.
   * @see ValidationUtils
   */
  @Override
  public void validate(Object target, Errors errors) {
    verifyArguments(target, errors, RequisitionGroupMessageKeys.ERROR_NULL);

    RequisitionGroupBaseDto group = (RequisitionGroupBaseDto) target;

    verifyProperties(group, errors);

    if (!errors.hasErrors()) {
      verifyCode(group.getId(), group.getCode(), errors);
      verifySupervisoryNode(group, errors);
      verifyFacilities(Optional.ofNullable(group.getMemberFacilities())
          .orElse(Collections.emptySet()).stream().map(facility -> (FacilityDto) facility)
          .collect(Collectors.toList()), errors);
      verifyProgramSchedules(Optional.ofNullable(group.getRequisitionGroupProgramSchedules())
          .orElse(Collections.emptyList()), errors);
    }
  }

  private void verifyProperties(RequisitionGroupBaseDto group, Errors errors) {
    // the Requisition Group Code is required, length 50 characters
    rejectIfEmptyOrWhitespace(errors, CODE, RequisitionGroupMessageKeys.ERROR_CODE_REQUIRED);

    // the requisition group name is required, length 50 characters
    rejectIfEmptyOrWhitespace(errors, NAME, RequisitionGroupMessageKeys.ERROR_NAME_REQUIRED);

    // the supervisory node is required
    rejectIfEmpty(errors, SUPERVISORY_NODE,
        RequisitionGroupMessageKeys.ERROR_SUPERVISORY_NODE_ID_REQUIRED);

    if (!errors.hasErrors()) {
      // the Requisition Group Code max length 50 characters
      if (group.getCode().length() > 50) {
        rejectValue(errors, CODE, RequisitionGroupMessageKeys.ERROR_CODE_TOO_LONG);
      }

      // the requisition group name max length 50 characters
      if (group.getName().length() > 50) {
        rejectValue(errors, NAME, RequisitionGroupMessageKeys.ERROR_NAME_TOO_LONG);
      }

      // description max length 250 characters
      if (null != group.getDescription() && group.getDescription().length() > 250) {
        rejectValue(errors, DESCRIPTION, RequisitionGroupMessageKeys.ERROR_DESCRIPTION_TOO_LONG);
      }
    }
  }

  private void verifyCode(UUID id, String code, Errors errors) {
    // requisition group code cannot be duplicated
    RequisitionGroup db = requisitionGroups.findByCode(code);

    if (null != db && (null == id || !id.equals(db.getId()))) {
      rejectValue(errors, CODE, RequisitionGroupMessageKeys.ERROR_CODE_DUPLICATED);
    }
  }

  private void verifySupervisoryNode(RequisitionGroupBaseDto group, Errors errors) {
    // supervisory node matches a defined supervisory node
    SupervisoryNodeBaseDto supervisoryNode = group.getSupervisoryNode();
    SupervisoryNode existing = supervisoryNodes.findOne(supervisoryNode.getId());
    if (null == supervisoryNode.getId()) {
      rejectValue(errors, SUPERVISORY_NODE,
          RequisitionGroupMessageKeys.ERROR_SUPERVISORY_NODE_ID_REQUIRED);
    } else if (null == existing) {
      rejectValue(errors, SUPERVISORY_NODE,
          RequisitionGroupMessageKeys.ERROR_SUPERVISORY_NODE_NON_EXISTENT);
    } else if (null != group.getId() && null != existing.getRequisitionGroup()
          && null != existing.getRequisitionGroup().getId()
          && !(existing.getRequisitionGroup().getId().equals(group.getId()))) {
      rejectValue(errors, SUPERVISORY_NODE,
          RequisitionGroupMessageKeys.ERROR_SUPERVISORY_NODE_ASSIGNED);
    } else if (null == group.getId() && null != existing.getRequisitionGroup()
          && null != existing.getRequisitionGroup().getId()) {
      rejectValue(errors, SUPERVISORY_NODE,
          RequisitionGroupMessageKeys.ERROR_SUPERVISORY_NODE_ASSIGNED);
    }
  }

  private void verifyFacilities(List<FacilityDto> memberFacilities, Errors errors) {
    // facilities must already exist in the system (cannot add new facilities from this point)
    for (FacilityDto facility : memberFacilities) {
      if (null == facility) {
        rejectValue(errors, MEMBER_FACILITIES, RequisitionGroupMessageKeys.ERROR_FACILITY_NULL);
      } else if (null == facility.getId()) {
        rejectValue(errors, MEMBER_FACILITIES,
            RequisitionGroupMessageKeys.ERROR_FACILITY_ID_REQUIRED);
      } else if (null == this.facilities.findOne(facility.getId())) {
        rejectValue(errors, MEMBER_FACILITIES,
            RequisitionGroupMessageKeys.ERROR_FACILITY_NON_EXISTENT);
      }
    }
  }

  private void verifyProgramSchedules(
      List<RequisitionGroupProgramSchedule.Importer> schedules, Errors errors) {
    // each program schedule must point to different existent program
    for (RequisitionGroupProgramSchedule.Importer schedule : schedules) {
      if (null == schedule) {
        rejectValue(errors, REQUISITION_GROUP_PROGRAM_SCHEDULES,
            RequisitionGroupMessageKeys.ERROR_PROGRAM_SCHEDULE_NULL);
      } else if (null == schedule.getProgram()) {
        rejectValue(errors, REQUISITION_GROUP_PROGRAM_SCHEDULES,
            RequisitionGroupMessageKeys.ERROR_PROGRAM_SCHEDULE_PROGRAM_NULL);
      } else if (null == schedule.getProgram().getId()) {
        rejectValue(errors, REQUISITION_GROUP_PROGRAM_SCHEDULES,
            RequisitionGroupMessageKeys.ERROR_PROGRAM_SCHEDULE_PROGRAM_ID_REQUIRED);
      } else if (null == programs.findOne(schedule.getProgram().getId())) {
        rejectValue(errors, REQUISITION_GROUP_PROGRAM_SCHEDULES,
            RequisitionGroupMessageKeys.ERROR_PROGRAM_SCHEDULE_PROGRAM_NON_EXISTENT);
      }
    }

    if (!errors.hasFieldErrors(REQUISITION_GROUP_PROGRAM_SCHEDULES)) {
      verifyProgramSchedulesProgramUniqueness(schedules, errors);
    }
  }

  private void verifyProgramSchedulesProgramUniqueness(
      List<RequisitionGroupProgramSchedule.Importer> schedules, Errors errors) {
    // programs must be unique for program schedules within requisition group
    Set<UUID> ids = schedules
        .stream()
        .map(schedule -> schedule.getProgram().getId())
        .collect(Collectors.toSet());

    if (ids.size() < schedules.size()) {
      rejectValue(errors, REQUISITION_GROUP_PROGRAM_SCHEDULES,
          RequisitionGroupMessageKeys.ERROR_PROGRAM_SCHEDULE_PROGRAM_DUPLICATED);
    }
  }
}
