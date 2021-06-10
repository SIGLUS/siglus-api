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

package org.siglus.siglusapi;

import com.google.gson.internal.bind.TypeAdapters;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.javers.core.Javers;
import org.javers.core.MappingStyle;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.hibernate.integration.HibernateUnproxyObjectAccessHook;
import org.javers.repository.sql.ConnectionProvider;
import org.javers.repository.sql.DialectName;
import org.javers.repository.sql.JaversSqlRepository;
import org.javers.repository.sql.SqlRepositoryBuilder;
import org.javers.spring.boot.sql.JaversSqlProperties;
import org.javers.spring.jpa.TransactionalJaversBuilder;
import org.openlmis.fulfillment.i18n.FulfillmentExposedMessageSourceImpl;
import org.openlmis.requisition.i18n.RequisitionExposedMessageSourceImpl;
import org.openlmis.stockmanagement.i18n.StockmanagementExposedMessageSourceImpl;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.i18n.ExposedMessageSourceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

@SpringBootApplication
@EnableAsync
@EnableJpaAuditing
@ComponentScan(basePackages = {"org.siglus", "org.openlmis"})
@EntityScan(basePackages = {"org.siglus", "org.openlmis"})
@EnableJpaRepositories(basePackages = {"org.siglus", "org.openlmis"})
@PropertySource("classpath:application.properties")
@PropertySource("classpath:stockmanagement-application.properties")
@PropertySource("classpath:requisition-application.properties")
@PropertySource("classpath:fulfillment-application.properties")
@EnableAspectJAutoProxy
@EnableScheduling
@EnableRetry
@SuppressWarnings({"PMD.TooManyMethods"})
public class Application {

  private static final String CLASSPATH_MESSAGES = "classpath:messages";
  private static final String UTF_8 = "UTF-8";
  private static final String HIBERNATE_VALIDATION_MESSAGES =
      "classpath:org/hibernate/validator/ValidationMessages";

  @Value("${defaultLocale}")
  private Locale locale;

  @Autowired
  private DialectName dialectName;

  @Autowired
  private JaversSqlProperties javersProperties;

  @Value("${spring.jpa.properties.hibernate.default_schema}")
  private String preferredSchema;

  @Value("${time.zoneId}")
  private String timeZoneId;

  @Value("${redis.url}")
  private String redisUrl;

  @Value("${redis.port}")
  private int redisPort;

  @Value("${redis.password}")
  private String redisPassword;

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
  public ExposedMessageSourceImpl messageSource() {
    ExposedMessageSourceImpl messageSource = new ExposedMessageSourceImpl();
    messageSource.setBasename(CLASSPATH_MESSAGES);
    messageSource.setDefaultEncoding(UTF_8);
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }

  // copy fromGroups stockmanagement Application.java start

  /**
   * Creates new MessageSource.
   *
   * @return Created MessageSource.
   */
  @Bean
  public StockmanagementExposedMessageSourceImpl stockmanagementMessageSource() {
    StockmanagementExposedMessageSourceImpl messageSource =
        new StockmanagementExposedMessageSourceImpl();
    messageSource.setBasename(CLASSPATH_MESSAGES);
    messageSource.setDefaultEncoding(UTF_8);
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }
  // copy fromGroups stockmanagement Application.java end

  // copy fromGroups requisition Application.java start

  /**
   * Creates new MessageSource.
   *
   * @return Created MessageSource.
   */
  @Bean
  public RequisitionExposedMessageSourceImpl requisitionMessageSource() {
    RequisitionExposedMessageSourceImpl messageSource = new RequisitionExposedMessageSourceImpl();
    messageSource.setBasename(CLASSPATH_MESSAGES);
    messageSource.setDefaultEncoding(UTF_8);
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }

  @Bean
  public Clock clock() {
    return Clock.system(ZoneId.of(timeZoneId));
  }
  // copy fromGroups requisition Application.java end

  // copy fromGroups referencedata Application.java start
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
        .registerValueGsonTypeAdapter(double.class, TypeAdapters.DOUBLE)
        .registerValueGsonTypeAdapter(Double.class, TypeAdapters.DOUBLE)
        .registerValueGsonTypeAdapter(float.class, TypeAdapters.FLOAT)
        .registerValueGsonTypeAdapter(Float.class, TypeAdapters.FLOAT)
        .build();
  }

  @Bean
  JedisConnectionFactory connectionFactory() {
    JedisConnectionFactory factory = new JedisConnectionFactory();
    factory.setHostName(redisUrl);
    factory.setPort(redisPort);
    factory.setPassword(redisPassword);
    factory.setUsePool(true);
    return factory;
  }

  @Bean
  ExecutorService executorService() {
    return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
  }

  @Bean
  RedisTemplate<Object, Object> redisTemplate(JedisConnectionFactory factory) {
    RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(factory);
    return redisTemplate;
  }
  // copy fromGroups referencedata Application.java end

  // copy fromGroups fulfillment Application.java start

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

  @Bean
  @Primary
  public FulfillmentExposedMessageSourceImpl fulfillmentMessageSource() {
    FulfillmentExposedMessageSourceImpl messageSource = new FulfillmentExposedMessageSourceImpl();
    messageSource.setBasename(CLASSPATH_MESSAGES);
    messageSource.setDefaultEncoding(UTF_8);
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }
  // copy fromGroups fulfillment Application.java end

  @Bean
  public AuditorAware<UUID> auditorAware(SiglusAuthenticationHelper authenticationHelper) {
    return () -> authenticationHelper.getCurrentUserId().orElse(null);
  }

  private MessageSource validationMessageSource() {
    ExposedMessageSourceImpl messageSource = new ExposedMessageSourceImpl();
    messageSource
        .setBasenames(HIBERNATE_VALIDATION_MESSAGES, CLASSPATH_MESSAGES);
    messageSource.setDefaultEncoding(UTF_8);
    // https://stackoverflow.com/questions/38714521/hibernate-expression-language-does-not-work
    // it works but i don't know why
    messageSource.setUseCodeAsDefaultMessage(false);
    return messageSource;
  }

  @Bean
  public LocalValidatorFactoryBean getValidator() {
    LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
    bean.setValidationMessageSource(validationMessageSource());
    return bean;
  }

  @Bean
  public RestTemplate remoteRestTemplate() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(60000);
    requestFactory.setReadTimeout(60000);
    return new RestTemplate(requestFactory);
  }

}
