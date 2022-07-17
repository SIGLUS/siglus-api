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

package org.siglus.siglusapi.service;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;

import java.util.Set;
import java.util.UUID;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.predicate.AbstractSimplePredicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.service.SiglusNotificationService.PredicateEvaluator;
import org.siglus.siglusapi.util.PermissionString;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public class SiglusNotificationServicePredicateEvaluatorTest {
  private final UUID nodeIdForInternalApprove = UUID.randomUUID();
  private final PermissionString fakePermissionString = new PermissionString("fake");
  @Mock private Root<Notification> root;
  @Mock private SiglusNotificationService service;
  @Mock private CriteriaBuilderImpl cb;

  @Before
  public void setup() {
    configureMockedCriteriaBuilder(cb);
  }

  @Test
  public void shouldReturnValidPredicateWhenEvalForShipmentsEdit() {
    // given
    given(service.getPredicateEvaluatorForShipmentsEdit()).willCallRealMethod();
    PredicateEvaluator evaluator = service.getPredicateEvaluatorForShipmentsEdit();
    Set<UUID> currentUserSupervisoryNodeIds = singleton(nodeIdForInternalApprove);
    // when
    Predicate predicate =
        evaluator.mapRightToPredicate(fakePermissionString, root, cb, null,
            currentUserSupervisoryNodeIds, true, null);

    // then
    assertThat(predicate.toString())
        .isEqualTo(
            "cb.and(\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, RECEIVED);,\n"
                + "    cb.equal(null, null);\n"
                + ");");
  }

  @Test
  public void shouldReturnValidPredicateWhenEvalForPodView() {
    // given
    given(service.getPredicateEvaluatorForPodView()).willCallRealMethod();
    PredicateEvaluator evaluator = service.getPredicateEvaluatorForPodView();
    Set<UUID> currentUserSupervisoryNodeIds = singleton(nodeIdForInternalApprove);
    // when
    Predicate predicate =
        evaluator.mapRightToPredicate(fakePermissionString, root, cb, null,
            currentUserSupervisoryNodeIds, true, null);

    // then
    assertThat(predicate.toString())
        .isEqualTo(
            "cb.and(\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, SHIPPED);,\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, UPDATE);\n"
                + ");");
  }

  @Test
  public void shouldReturnValidPredicateWhenEvalForPodManage() {
    // given
    given(service.getPredicateEvaluatorForPodManage()).willCallRealMethod();
    PredicateEvaluator evaluator = service.getPredicateEvaluatorForPodManage();
    Set<UUID> currentUserSupervisoryNodeIds = singleton(nodeIdForInternalApprove);
    // when
    Predicate predicate =
        evaluator.mapRightToPredicate(fakePermissionString, root, cb, null,
            currentUserSupervisoryNodeIds, true, null);

    // then
    assertThat(predicate.toString())
        .isEqualTo(
            "cb.and(\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, SHIPPED);,\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, TODO);\n"
                + ");");
  }

  @Test
  public void shouldReturnValidPredicateWhenEvalForStockCardsViewGivenCanEditShipments() {
    // given
    given(service.getPredicateEvaluatorForStockCardsView()).willCallRealMethod();
    PredicateEvaluator evaluator = service.getPredicateEvaluatorForStockCardsView();
    Set<UUID> currentUserSupervisoryNodeIds = singleton(nodeIdForInternalApprove);
    // when
    Predicate predicate =
        evaluator.mapRightToPredicate(fakePermissionString, root, cb, null,
            currentUserSupervisoryNodeIds, true, null);

    // then
    assertThat(predicate.toString())
        .isEqualTo(
            "cb.and(\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, ORDERED);,\n"
                + "    cb.equal(null, null);\n"
                + ");");
  }

  @Test
  public void shouldReturnValidPredicateWhenEvalForOrdersEdit() {
    // given
    given(service.getPredicateEvaluatorForOrdersEdit()).willCallRealMethod();
    PredicateEvaluator evaluator = service.getPredicateEvaluatorForOrdersEdit();
    Set<UUID> currentUserSupervisoryNodeIds = singleton(nodeIdForInternalApprove);
    // when
    Predicate predicate =
        evaluator.mapRightToPredicate(fakePermissionString, root, cb, null,
            currentUserSupervisoryNodeIds, false, null);

    // then
    assertThat(predicate.toString())
        .isEqualTo(
            "cb.and(\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, APPROVED);,\n"
                + "    cb.equal(null, null);\n"
                + ");");
  }

  @Test
  public void shouldReturnValidPredicateWhenEvalForRequisitionView() {
    // given
    given(service.getPredicateEvaluatorForRequisitionView()).willCallRealMethod();
    PredicateEvaluator evaluator = service.getPredicateEvaluatorForRequisitionView();
    Set<UUID> currentUserSupervisoryNodeIds = singleton(nodeIdForInternalApprove);
    Root<Notification> root = mock(Root.class);
    given(root.get("status")).willAnswer(invocation -> mock(Path.class));
    // when
    Predicate predicate =
        evaluator.mapRightToPredicate(fakePermissionString, root, cb, null,
            currentUserSupervisoryNodeIds, false, null);

    // then
    assertThat(predicate.toString())
        .isEqualTo(
            "cb.and(\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, null);,\n"
                + "    null,\n"
                + "    cb.equal(null, null);\n"
                + ");");
  }

  @Test
  public void shouldReturnValidPredicateWhenEvalForRequisitionCreate() {
    // given
    given(service.getPredicateEvaluatorForRequisitionCreate()).willCallRealMethod();
    PredicateEvaluator evaluator = service.getPredicateEvaluatorForRequisitionCreate();
    Set<UUID> currentUserSupervisoryNodeIds = singleton(nodeIdForInternalApprove);
    // when
    Predicate predicate =
        evaluator.mapRightToPredicate(fakePermissionString, root, cb, null,
            currentUserSupervisoryNodeIds, false, null);

    // then
    assertThat(predicate.toString())
        .isEqualTo(
            "cb.and(\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, REJECTED);,\n"
                + "    cb.equal(null, null);\n"
                + ");");
  }

  @Test
  public void shouldReturnValidPredicateWhenEvalForRequisitionAuthorize() {
    // given
    given(service.getPredicateEvaluatorForRequisitionAuthorize()).willCallRealMethod();
    PredicateEvaluator evaluator = service.getPredicateEvaluatorForRequisitionAuthorize();
    Set<UUID> currentUserSupervisoryNodeIds = singleton(nodeIdForInternalApprove);
    // when
    Predicate predicate =
        evaluator.mapRightToPredicate(fakePermissionString, root, cb, null,
            currentUserSupervisoryNodeIds, false, null);

    // then
    assertThat(predicate.toString())
        .isEqualTo(
            "cb.and(\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, SUBMITTED);,\n"
                + "    cb.equal(null, null);\n"
                + ");");
  }

  @Test
  public void shouldReturnNullWhenEvaluateForRequisitionApproveGivenCurrentUserSupervisoryNodeIdsIsEmpty() {
    // given
    given(service.getPredicateEvaluatorForRequisitionApprove()).willCallRealMethod();
    // when
    PredicateEvaluator evaluator = service.getPredicateEvaluatorForRequisitionApprove();
    Predicate predicate =
        evaluator.mapRightToPredicate(null, null, null, null,
            emptySet(), false, null);
    // then
    assertThat(predicate).isNull();
  }

  @Test
  public void
      shouldReturnInternalPredicateWhenEvalRequisitionApproveGivenNodeIdForInternalApproveIsNotEmpty() {
    // given
    given(service.getPredicateEvaluatorForRequisitionApprove()).willCallRealMethod();
    PredicateEvaluator evaluator = service.getPredicateEvaluatorForRequisitionApprove();
    given(service.findSupervisoryNodeIdForInternalApprove(any(), any(), any(), any())).willReturn(
        nodeIdForInternalApprove);
    Set<UUID> currentUserSupervisoryNodeIds = singleton(nodeIdForInternalApprove);

    // when
    Predicate predicate =
        evaluator.mapRightToPredicate(fakePermissionString, root, cb, null,
            currentUserSupervisoryNodeIds, false, null);

    // then
    assertThat(predicate.toString())
        .isEqualTo(
            "cb.and(\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, null);,\n"
                + "    cb.equal(null, AUTHORIZED);\n"
                + ");");
  }

  private void configureMockedCriteriaBuilder(CriteriaBuilderImpl cb) {
    given(cb.equal(any(Expression.class), any(Object.class)))
        .willAnswer(invocation -> new InvocationCaptureForTest(cb, invocation.toString()));
    given(cb.and(anyVararg()))
        .willAnswer(invocation -> new InvocationCaptureForTest(cb, invocation.toString()));
    given(cb.or(any(Expression.class), any(Expression.class)))
        .willAnswer(invocation -> new InvocationCaptureForTest(cb, invocation.toString()));
  }

  private static class InvocationCaptureForTest extends AbstractSimplePredicate {

    private final String expr;

    public InvocationCaptureForTest(CriteriaBuilderImpl criteriaBuilder, String expr) {
      super(criteriaBuilder);
      this.expr = expr;
    }

    @Override
    public String render(boolean isNegated, RenderingContext renderingContext) {
      return null;
    }

    @Override
    public void registerParameters(ParameterRegistry registry) {
    }

    @Override
    public String toString() {
      return expr;
    }
  }
}
