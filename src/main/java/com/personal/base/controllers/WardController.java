package com.personal.base.controllers;

import com.personal.base.dto.address.WardResponse;
import com.personal.base.services.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/wards")
public class WardController {

  @Autowired
  private AddressService addressService;

  @GetMapping
  public ResponseEntity<List<WardResponse>> listAllWards() {
    return ResponseEntity.ok(addressService.listAllWards());
  }
}
