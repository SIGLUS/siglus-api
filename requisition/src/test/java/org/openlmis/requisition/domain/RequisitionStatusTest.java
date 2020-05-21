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

package org.openlmis.requisition.domain;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;

@RunWith(Parameterized.class)
public class RequisitionStatusTest {

  private RequisitionStatus status;
  private boolean preAuthorize;
  private boolean postSubmitted;
  private boolean submittable;
  private boolean updatable;

  /**
   * Creates a new instance of RequisitionStatusTest. It is used by JUnit to create a parameterized
   * test. It should not be used manually.
   *
   * @param status        requisition status
   * @param preAuthorize  flag that is used to check if the given status should be preAuthorize
   * @param postSubmitted flag that is used to check if the given status should be postSubmitted
   */
  public RequisitionStatusTest(RequisitionStatus status, boolean preAuthorize,
                               boolean postSubmitted, boolean submittable, boolean updatable) {
    this.status = status;
    this.preAuthorize = preAuthorize;
    this.postSubmitted = postSubmitted;
    this.submittable = submittable;
    this.updatable = updatable;
  }

  /**
   * Creates data needed to initialize each test.
   *
   * @return a collections of arrays that contain data needed to create a new instance of the test.
   */
  @Parameterized.Parameters(name = "status = {0}, preAuthorize = {1}, postSubmitted = {2}, "
      + "submittable = {3}, updatable = {4}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {RequisitionStatus.INITIATED, true, false, true, true},
        {RequisitionStatus.REJECTED, true, false, true, true},
        {RequisitionStatus.SUBMITTED, true, true, false, true},
        {RequisitionStatus.AUTHORIZED, false, true, false,true},
        {RequisitionStatus.APPROVED, false, true, false, false},
        {RequisitionStatus.RELEASED, false, true, false, false},
        {RequisitionStatus.SKIPPED, false, false, false, false}
    });
  }

  @Test
  public void shouldHaveCorrectValueForPreAuthorize() throws Exception {
    assertThat(status.isPreAuthorize(), is(equalTo(preAuthorize)));
  }

  @Test
  public void shouldHaveCorrectValueForPostSubmitted() throws Exception {
    assertThat(status.isPostSubmitted(), is(equalTo(postSubmitted)));
  }

  @Test
  public void shouldHaveCorrectValueForSubmittable() throws Exception {
    assertThat(status.isSubmittable(), is(equalTo(submittable)));
  }

  @Test
  public void shouldHaveCorrectValueForUpdatable() throws Exception {
    assertThat(status.isUpdatable(), is(equalTo(updatable)));
  }
}
