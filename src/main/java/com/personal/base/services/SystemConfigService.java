package com.personal.base.services;

import com.personal.base.models.SystemConfig;
import com.personal.base.models.type.UploadMethod;
import com.personal.base.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemConfigService {

  private static final long CONFIG_ID = 1L;

  @Autowired
  private SystemConfigRepository systemConfigRepository;

  public UploadMethod getUploadMethod() {
    return getOrCreateConfig().getUploadMethod();
  }

  public UploadMethod updateUploadMethod(UploadMethod uploadMethod) {
    SystemConfig config = getOrCreateConfig();
    config.setUploadMethod(uploadMethod);
    return systemConfigRepository.save(config).getUploadMethod();
  }

  private SystemConfig getOrCreateConfig() {
    return systemConfigRepository.findById(CONFIG_ID)
            .orElseGet(() -> systemConfigRepository.save(new SystemConfig(CONFIG_ID, UploadMethod.MINIO)));
  }
}
