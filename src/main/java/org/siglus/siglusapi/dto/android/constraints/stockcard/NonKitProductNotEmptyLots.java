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

package org.siglus.siglusapi.dto.android.constraints.stockcard;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import org.siglus.siglusapi.dto.android.validators.stockcard.NonKitProductNotEmptyLotsValidator;

@Target({METHOD, FIELD, TYPE, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = NonKitProductNotEmptyLotsValidator.class)
public @interface NonKitProductNotEmptyLots {

  String message() default "{org.siglus.siglusapi.dto.android.constraints.stockcard.NonKitProductNotEmptyLots.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

}
