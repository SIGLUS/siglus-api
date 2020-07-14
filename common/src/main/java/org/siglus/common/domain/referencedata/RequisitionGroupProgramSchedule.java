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

package org.siglus.common.domain.referencedata;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.TypeName;
import org.siglus.common.domain.BaseEntity;

/**
 * RequisitionGroupProgramSchedule represents the schedule to be mapped for a given program and
 * requisition group.
 */
@Entity
@Table(name = "requisition_group_program_schedules", schema = "referencedata",
    uniqueConstraints = @UniqueConstraint(
        name = "requisition_group_program_schedule_unique_program_requisitionGroup",
        columnNames = {"requisitionGroupId", "programId"}))
@NoArgsConstructor
@TypeName("RequisitionGroupProgramSchedule")
public class RequisitionGroupProgramSchedule extends BaseEntity {

  @ManyToOne
  @JoinColumn(name = "requisitionGroupId", nullable = false)
  @Getter
  @Setter
  private RequisitionGroup requisitionGroup;

  @OneToOne
  @JoinColumn(name = "programId", nullable = false)
  @Getter
  @Setter
  private Program program;

  @OneToOne
  @JoinColumn(name = "processingScheduleId", nullable = false)
  @Getter
  @Setter
  private ProcessingSchedule processingSchedule;

  @Column(nullable = false)
  @Getter
  @Setter
  private boolean directDelivery;

  @OneToOne
  @JoinColumn(name = "dropOffFacilityId")
  @Getter
  @Setter
  private Facility dropOffFacility;

  private RequisitionGroupProgramSchedule(RequisitionGroup requisitionGroup, Program program,
                                          ProcessingSchedule schedule, boolean directDelivery) {
    this.requisitionGroup = requisitionGroup;
    this.program = Objects.requireNonNull(program);
    this.processingSchedule = Objects.requireNonNull(schedule);
    this.directDelivery = directDelivery;
  }

  public static RequisitionGroupProgramSchedule newRequisitionGroupProgramSchedule(
      RequisitionGroup requisitionGroup, Program program, ProcessingSchedule schedule,
      boolean directDelivery) {
    return new RequisitionGroupProgramSchedule(requisitionGroup, program, schedule, directDelivery);
  }

  /**
   * Copy properties from the given instance.
   *
   * @param instance an instance whose properties will be applied to this object
   */
  public void updateFrom(RequisitionGroupProgramSchedule instance) {
    requisitionGroup = instance.getRequisitionGroup();
    program = instance.getProgram();
    processingSchedule = instance.getProcessingSchedule();
    directDelivery = instance.isDirectDelivery();
    dropOffFacility = instance.getDropOffFacility();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RequisitionGroupProgramSchedule)) {
      return false;
    }
    RequisitionGroupProgramSchedule that = (RequisitionGroupProgramSchedule) obj;
    return Objects.equals(requisitionGroup, that.requisitionGroup)
        && Objects.equals(program, that.program)
        && Objects.equals(processingSchedule, that.processingSchedule);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requisitionGroup, program, processingSchedule);
  }

}
