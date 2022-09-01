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

package org.siglus.siglusapi.localmachine;

import javax.transaction.Transactional;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.runner.RunWith;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {LocalMachineTestConfig.class})
//@ComponentScan(basePackageClasses = {LocalMachineTestConfig.class})
@DataJpaTest
@AutoConfigureTestDatabase(
    replace = Replace.AUTO_CONFIGURED,
    connection = EmbeddedDatabaseConnection.H2)
@EnableAutoConfiguration
@Transactional
public abstract class LocalMachineIntegrationTest {
  @MockBean protected SiglusAuthenticationHelper authenticationHelper;
  @MockBean protected LockProvider lockProvider;
}
