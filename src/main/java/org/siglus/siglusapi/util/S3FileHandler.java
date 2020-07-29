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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import javax.annotation.PostConstruct;
import org.openlmis.requisition.exception.ServerException;
import org.siglus.siglusapi.i18n.SimamMessageKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;


@Component
public class S3FileHandler {

  public static final String FOLDER_SUFFIX = "/";

  @Value("${aws.access.key}")
  private String accessKey;

  @Value("${aws.secret.access.key}")
  private String secretKey;

  @Value("${aws.region}")
  private String region;

  @Value("${email.attachment.s3.bucket}")
  private String bucketName;

  @Value("${email.attachment.s3.bucket.folder}")
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

  public void uploadFileToS3(String filePath, String fileName) {
    File file;
    try {
      file = ResourceUtils.getFile(filePath);
    } catch (FileNotFoundException e) {
      throw new ServerException(e, SimamMessageKeys.ERROR_FILE_NOT_FOUND, fileName);
    }
    String keyName = bucketFolder + FOLDER_SUFFIX + fileName;
    s3Client.putObject(bucketName, keyName, file);
  }
}