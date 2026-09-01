package com.personal.base.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lets the admin cache-timing test page reset the Redis cache before a run,
 * so the next call is a guaranteed DB hit ("before Redis") and the call after
 * that is a guaranteed cache hit ("after Redis").
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/cache-test")
public class CacheTestController {

  private static final List<String> CACHE_NAMES = List.of("provinces", "wards", "users");

  @Autowired
  private CacheManager cacheManager;

  @PostMapping("/clear")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> clearCaches() {
    for (String name : CACHE_NAMES) {
      Cache cache = cacheManager.getCache(name);
      if (cache != null) {
        cache.clear();
      }
    }
    return ResponseEntity.noContent().build();
  }
}
