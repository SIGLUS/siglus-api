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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.internal.bind.TypeAdapters;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Locale;
import javax.annotation.PostConstruct;
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
import org.openlmis.referencedata.domain.BaseEntity;
import org.openlmis.referencedata.i18n.ReferencedataExposedMessageSourceImpl;
import org.openlmis.referencedata.security.UserNameProvider;
import org.openlmis.referencedata.validate.ProcessingPeriodValidator;
import org.openlmis.referencedata.web.csv.processor.FormatCommodityType;
import org.openlmis.referencedata.web.csv.processor.FormatProcessingPeriod;
import org.openlmis.referencedata.web.csv.processor.ParseCommodityType;
import org.openlmis.referencedata.web.csv.processor.ParseProcessingPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.togglz.core.manager.EnumBasedFeatureProvider;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.spi.FeatureProvider;
import org.togglz.redis.RedisStateRepository;
import redis.clients.jedis.JedisPool;

@SpringBootApplication(scanBasePackages = "org.openlmis")
@ImportResource("classpath*:/applicationContext.xml")
@EntityScan(basePackageClasses = BaseEntity.class)
@EnableAsync
@EnableCaching
@SuppressWarnings({"PMD.TooManyMethods"})
public class Application {

  private Logger logger = LoggerFactory.getLogger(Application.class);

  @Value("${defaultLocale}")
  private Locale locale;

  @Value("${time.zoneId}")
  private String timeZoneId;

  @Value("${redis.url}")
  private String redisUrl;

  @Value("${redis.port}")
  private int redisPort;

  @Value("${redis.password}")
  private String redisPassword;

  @Autowired
  DialectName dialectName;

  @Autowired
  private JaversProperties javersProperties;

  @Value("${spring.jpa.properties.hibernate.default_schema}")
  private String preferredSchema;

  @Value("${referencedata.csv.separator}")
  private String separator;

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

    ReferencedataJaVersDateProvider customDateProvider = new ReferencedataJaVersDateProvider();

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

  /**
   * Configures the Flyway migration strategy to clean the DB before migration first.  This is used
   * as the default unless the Spring Profile "production" is active.
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
   * Creates new MessageSource.
   *
   * @return Created MessageSource.
   */
  @Bean
  public ReferencedataExposedMessageSourceImpl messageSource() {
    ReferencedataExposedMessageSourceImpl messageSource = new ReferencedataExposedMessageSourceImpl();
    messageSource.setBasename("classpath:messages");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }

  @Bean
  public ProcessingPeriodValidator beforeCreatePeriodValidator() {
    return new ProcessingPeriodValidator();
  }

  @Bean
  public ProcessingPeriodValidator beforeSavePeriodValidator() {
    return new ProcessingPeriodValidator();
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
  public StringRedisSerializer stringRedisSerializer() {
    return new StringRedisSerializer();
  }

  /**
   * Creates RedisTemplate instance.
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(JedisConnectionFactory factory,
      ObjectMapper objectMapper) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    Jackson2JsonRedisSerializer jackson2JsonRedisSerializer =
        new Jackson2JsonRedisSerializer<>(Object.class);
    redisTemplate.setConnectionFactory(factory);
    redisTemplate.setKeySerializer(stringRedisSerializer());
    redisTemplate.setDefaultSerializer(jackson2JsonRedisSerializer);
    redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
    redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
    jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

    return redisTemplate;
  }

  /**
   * Creates RedisCacheManager instance.
   */
  @Bean
  public RedisCacheManager cacheManager(ObjectMapper objectMapper) {
    RedisCacheManager redisCacheManager = new RedisCacheManager(redisTemplate(connectionFactory(),
        objectMapper));
    redisCacheManager.setTransactionAware(true);
    redisCacheManager.setLoadRemoteCachesOnStartup(true);
    redisCacheManager.setUsePrefix(true);
    return redisCacheManager;
  }

  @Bean
  public FeatureProvider featureProvider() {
    return new EnumBasedFeatureProvider(AvailableFeatures.class);
  }

  @Bean
  StateRepository getStateRepository() {
    return new RedisStateRepository.Builder()
        .keyPrefix("togglz:")
        .jedisPool(new JedisPool(redisUrl, redisPort))
        .build();
  }

  /**
   * Sets separator field for csv parsers/formatters.
   */
  @PostConstruct
  public void setCsvSeparator() {
    ParseCommodityType.SEPARATOR = separator;
    FormatCommodityType.SEPARATOR = separator;
    ParseProcessingPeriod.SEPARATOR = separator;
    FormatProcessingPeriod.SEPARATOR = separator;
  }
}
