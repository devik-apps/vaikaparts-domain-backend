package com.devikapps.vaikaparts.exception.bucket;

import com.devikapps.vaikaparts.InfraGenerated;

@InfraGenerated
public class BucketDownloadException extends BucketOperationException {

  public BucketDownloadException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String getErrorCode() {
    return "BUCKET_DOWNLOAD_FAILED";
  }
}
