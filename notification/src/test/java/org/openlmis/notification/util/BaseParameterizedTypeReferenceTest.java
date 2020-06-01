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

package org.openlmis.notification.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nl.jqno.equalsverifier.internal.checkers.AbstractDelegationChecker;
import nl.jqno.equalsverifier.internal.checkers.CachedHashCodeChecker;
import nl.jqno.equalsverifier.internal.checkers.ExamplesChecker;
import nl.jqno.equalsverifier.internal.checkers.FieldsChecker;
import nl.jqno.equalsverifier.internal.checkers.NullChecker;
import nl.jqno.equalsverifier.internal.checkers.SignatureChecker;
import nl.jqno.equalsverifier.internal.prefabvalues.JavaApiPrefabValues;
import nl.jqno.equalsverifier.internal.prefabvalues.TypeTag;
import nl.jqno.equalsverifier.internal.reflection.ClassAccessor;
import nl.jqno.equalsverifier.internal.util.Configuration;
import org.junit.Test;

public class BaseParameterizedTypeReferenceTest {

  @Test
  public void equalsContract() {
    Configuration<BaseParameterizedTypeReference> config = Configuration
        .of(BaseParameterizedTypeReference.class)
        .withRedefinedSuperclass()
        .withNonnullFields(Collections.singletonList("type"));

    JavaApiPrefabValues.addTo(config.getPrefabValues());

    TypeTag tag = config.getTypeTag();
    ClassAccessor<BaseParameterizedTypeReference> classAccessor = config.createClassAccessor();

    List<BaseParameterizedTypeReference> unequalExamples = new ArrayList<>();
    unequalExamples.add(classAccessor.getRedObject(tag));
    unequalExamples.add(classAccessor.getBlackObject(tag));
    config = config.withUnequalExamples(unequalExamples);

    new SignatureChecker<>(config).check();
    new AbstractDelegationChecker<>(config).check();
    new NullChecker<>(config).check();
    new CachedHashCodeChecker<>(config).check();
    new ExamplesChecker<>(config).check();
    // disabled for now
    // new HierarchyChecker<>(config).check();
    new FieldsChecker<>(config).check();
  }

}
