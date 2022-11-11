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

package org.siglus.siglusapi.util;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.exception.ServerException;
import org.siglus.siglusapi.i18n.MessageKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;


@Component
@Slf4j
public class S3FileHandler {

  public static final String FOLDER_SUFFIX = "/";
  private static final int SEVEN_DAYS = 1000 * 60 * 60 * 24 * 7;

  @Value("${aws.access.key}")
  private String accessKey;

  @Value("${aws.secret.access.key}")
  private String secretKey;

  @Value("${aws.region}")
  private String region;

  @Value("${s3.bucket.name}")
  private String bucketName;

  @Value("${s3.bucket.folder}")
  private String bucketFolder;

  private AmazonS3 s3Client;

  @PostConstruct
  public void init() {
    AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
    s3Client = AmazonS3ClientBuilder
        .standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withRegion(region)
        .build();
  }

  public void deleteFileFromS3(String fileName) {
    String keyName = getKeyName(fileName);
    s3Client.deleteObject(bucketName, keyName);
  }

  public void uploadFileToS3(String filePath, String fileName) {
    File file;
    try {
      file = ResourceUtils.getFile(filePath);
    } catch (FileNotFoundException e) {
      throw new ServerException(e, MessageKeys.ERROR_FILE_NOT_FOUND, fileName);
    }
    String keyName = getKeyName(fileName);
    log.debug("bucketName: {}, keyName: {}", bucketName, keyName);
    s3Client.putObject(bucketName, keyName, file);
    try {
      Files.delete(Paths.get(filePath));
    } catch (IOException e) {
      log.error("Delete file: {} with error {}", fileName, e.getMessage());
    }
  }

  public String getUrlFromS3(String fileName) {
    java.util.Date expiration = new java.util.Date();
    long expTimeMillis = Instant.now().toEpochMilli();
    expTimeMillis += SEVEN_DAYS;
    expiration.setTime(expTimeMillis);
    String keyName = getKeyName(fileName);
    log.debug("bucketName: {}, keyName: {}", bucketName, keyName);
    GeneratePresignedUrlRequest generatePresignedUrlRequest =
        new GeneratePresignedUrlRequest(bucketName, keyName)
            .withMethod(HttpMethod.GET)
            .withExpiration(expiration);
    URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    if (url != null) {
      return url.toString();
    }
    return null;
  }

  private String getKeyName(String fileName) {
    return bucketFolder + FOLDER_SUFFIX + fileName;
  }
}