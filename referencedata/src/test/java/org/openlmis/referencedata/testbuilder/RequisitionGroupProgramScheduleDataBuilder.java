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

package org.openlmis.referencedata.testbuilder;

import java.util.UUID;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;

public class RequisitionGroupProgramScheduleDataBuilder {

  private UUID id = UUID.randomUUID();
  private RequisitionGroup requisitionGroup = new RequisitionGroupDataBuilder().build();
  private Program program = new ProgramDataBuilder().build();
  private ProcessingSchedule processingSchedule = new ProcessingScheduleDataBuilder().build();
  private boolean directDelivery = true;
  private Facility dropOffFacility = new FacilityDataBuilder().build();

  public RequisitionGroupProgramScheduleDataBuilder withProgram(Program program) {
    this.program = program;
    return this;
  }

  public RequisitionGroupProgramScheduleDataBuilder withSchedule(ProcessingSchedule schedule) {
    this.processingSchedule = schedule;
    return this;
  }

  public RequisitionGroupProgramScheduleDataBuilder withRequisitionGroup(
      RequisitionGroup requisitionGroup) {
    this.requisitionGroup = requisitionGroup;
    return this;
  }

  public RequisitionGroupProgramScheduleDataBuilder withDropOffFacility(Facility facility) {
    this.dropOffFacility = facility;
    return this;
  }

  /**
   * Builds new instance of {@link RequisitionGroupProgramSchedule} based on data from the builder.
   */
  public RequisitionGroupProgramSchedule build() {
    RequisitionGroupProgramSchedule schedule = buildAsNew();
    schedule.setId(id);

    return schedule;
  }

  /**
   * Builds new instance of {@link RequisitionGroupProgramSchedule} based on data from the builder.
   * Compares to {@link #build()} the result instance will not have a value in the id field.
   */
  public RequisitionGroupProgramSchedule buildAsNew() {
    RequisitionGroupProgramSchedule schedule = new RequisitionGroupProgramSchedule();
    schedule.setRequisitionGroup(requisitionGroup);
    schedule.setProgram(program);
    schedule.setProcessingSchedule(processingSchedule);
    schedule.setDirectDelivery(directDelivery);
    schedule.setDropOffFacility(dropOffFacility);

    return schedule;
  }

}
