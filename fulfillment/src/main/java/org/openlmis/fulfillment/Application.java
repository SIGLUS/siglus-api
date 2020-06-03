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

package org.openlmis.fulfillment;

import com.google.gson.internal.bind.TypeAdapters;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Locale;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.javers.core.Javers;
import org.javers.core.MappingStyle;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.hibernate.integration.HibernateUnproxyObjectAccessHook;
import org.javers.repository.sql.ConnectionProvider;
import org.javers.repository.sql.DialectName;
import org.javers.repository.sql.JaversSqlRepository;
import org.javers.repository.sql.SqlRepositoryBuilder;
import org.javers.spring.auditable.AuthorProvider;
import org.javers.spring.boot.sql.JaversProperties;
import org.javers.spring.jpa.TransactionalJaversBuilder;
import org.openlmis.fulfillment.domain.BaseEntity;
import org.openlmis.fulfillment.i18n.ExposedMessageSource;
import org.openlmis.fulfillment.i18n.ExposedMessageSourceImpl;
import org.openlmis.fulfillment.security.UserNameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

@SuppressWarnings("PMD.TooManyMethods")
@SpringBootApplication(scanBasePackages = "org.openlmis.fulfillment")
@EntityScan(basePackageClasses = BaseEntity.class)
public class Application {

  private static final Logger logger = LoggerFactory.getLogger(Application.class);

  @Value("${defaultLocale}")
  private Locale locale;

  @Value("${time.zoneId}")
  private String timeZoneId;

  @Value("${spring.jpa.properties.hibernate.default_schema}")
  private String preferredSchema;

  @Autowired
  DialectName dialectName;

  @Autowired
  private JaversProperties javersProperties;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  /**
   * Creates new LocaleResolver.
   *
   * @return Created LocalResolver.
   */
  @Bean
  public LocaleResolver localeResolver() {
    CookieLocaleResolver lr = new CookieLocaleResolver();
    lr.setCookieName("lang");
    lr.setDefaultLocale(locale);
    return lr;
  }

  /**
   * Creates new MessageSource.
   *
   * @return Created MessageSource.
   */
  @Bean
  public ExposedMessageSource messageSource() {
    ExposedMessageSourceImpl messageSource = new ExposedMessageSourceImpl();
    messageSource.setBasename("classpath:messages");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setUseCodeAsDefaultMessage(true);

    return messageSource;
  }

  /**
   * Creates new LocalValidatorFactoryBean.
   *
   * @return Created LocalValidatorFactoryBean.
   */
  @Bean
  public LocalValidatorFactoryBean validator() {
    return new LocalValidatorFactoryBean();
  }

  /**
   * Creates new camelContext.
   *
   * @return Created camelContext.
   */
  @Bean
  public CamelContext camelContext() {
    return new DefaultCamelContext();
  }

  /**
   * Creates new camelTemplate.
   *
   * @return Created camelTemplate.
   */
  @Bean
  public ProducerTemplate camelTemplate() {
    return camelContext().createProducerTemplate();
  }

  /**
   * Creates new Clock.
   *
   * @return Created clock.
   */
  @Bean
  public Clock clock() {
    return Clock.system(ZoneId.of(timeZoneId));
  }

  /**
   * Configures the Flyway migration strategy to clean the DB before migration first.  This is used
   * as the default unless the Spring Profile "production" is active.
   *
   * @return the clean-migrate strategy
   */
  @Bean
  @Profile("!production")
  public FlywayMigrationStrategy cleanMigrationStrategy() {
    return flyway -> {
      logger.info("Using clean-migrate flyway strategy -- production profile not active");
      flyway.setCallbacks(flywayCallback());
      flyway.clean();
      flyway.migrate();
    };
  }

  @Bean
  public FlywayCallback flywayCallback() {
    return new ExportSchemaFlywayCallback();
  }

  /**
   * Create and return a UserNameProvider. By default, if we didn't do so, an instance of
   * SpringSecurityAuthorProvider would automatically be created and returned instead.
   */
  @Bean
  public AuthorProvider authorProvider() {
    return new UserNameProvider();
  }

  /**
   * Create and return an instance of JaVers precisely configured as necessary.
   * This is particularly helpful for getting JaVers to create and use tables
   * within a particular schema (specified via the withSchema method).
   *
   * @See <a href="https://github.com/javers/javers/blob/master/javers-spring-boot-starter-sql/src
   * /main/java/org/javers/spring/boot/sql/JaversSqlAutoConfiguration.java">
   * JaversSqlAutoConfiguration.java</a> for the default configuration upon which this code is based
   */
  @Bean
  public Javers javersProvider(ConnectionProvider connectionProvider,
      PlatformTransactionManager transactionManager) {
    JaversSqlRepository sqlRepository = SqlRepositoryBuilder
        .sqlRepository()
        .withConnectionProvider(connectionProvider)
        .withDialect(dialectName)
        .withSchema(preferredSchema)
        .build();

    JaVersDateProvider customDateProvider = new JaVersDateProvider();

    return TransactionalJaversBuilder
        .javers()
        .withTxManager(transactionManager)
        .registerJaversRepository(sqlRepository)
        .withObjectAccessHook(new HibernateUnproxyObjectAccessHook())
        .withListCompareAlgorithm(
            ListCompareAlgorithm.valueOf(javersProperties.getAlgorithm().toUpperCase()))
        .withMappingStyle(
            MappingStyle.valueOf(javersProperties.getMappingStyle().toUpperCase()))
        .withNewObjectsSnapshot(javersProperties.isNewObjectSnapshot())
        .withPrettyPrint(javersProperties.isPrettyPrint())
        .withTypeSafeValues(javersProperties.isTypeSafeValues())
        .withPackagesToScan(javersProperties.getPackagesToScan())
        .withDateTimeProvider(customDateProvider)
        .registerValueGsonTypeAdapter(double.class, TypeAdapters.DOUBLE)
        .registerValueGsonTypeAdapter(Double.class, TypeAdapters.DOUBLE)
        .registerValueGsonTypeAdapter(float.class, TypeAdapters.FLOAT)
        .registerValueGsonTypeAdapter(Float.class, TypeAdapters.FLOAT)
        .build();
  }
}
