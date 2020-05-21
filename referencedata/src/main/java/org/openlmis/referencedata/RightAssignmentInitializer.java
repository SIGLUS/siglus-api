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

package org.openlmis.referencedata;

import java.util.concurrent.Future;
import org.openlmis.referencedata.service.RightAssignmentService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RightAssignmentInitializer runs after its associated Spring application has loaded. It 
 * automatically re-generates right assignments into the database, after dropping the existing 
 * right assignments. This component only runs when the "refresh-db" Spring profile is set.
 */
@Component
@Profile("refresh-db")
@Order(10)
public class RightAssignmentInitializer implements CommandLineRunner {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
      RightAssignmentInitializer.class);
  
  @Autowired
  RightAssignmentService rightAssignmentService;
  
  /**
   * Re-generates right assignments.
   * @param args command line arguments
   */
  public void run(String... args) throws InterruptedException {
    XLOGGER.entry();
    
    Future<Void> result = rightAssignmentService.regenerateRightAssignments();

    // Wait until it finishes
    while (true) {
      if (result.isDone()) {
        break;
      }
      Thread.sleep(1000);
    }

    XLOGGER.debug("Finished regenerating right assignments");
    
    XLOGGER.exit();
  }
}