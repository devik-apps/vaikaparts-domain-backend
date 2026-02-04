package com.devikapps.vaikaparts.exception.bucket;

import com.devikapps.vaikaparts.InfraGenerated;

@InfraGenerated
public class BucketDirectoryUploadException extends BucketOperationException {

  public BucketDirectoryUploadException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "BUCKET_DIRECTORY_UPLOAD_FAILED";
  }
}
