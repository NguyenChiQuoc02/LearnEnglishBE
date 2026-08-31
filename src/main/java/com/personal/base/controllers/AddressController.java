package com.personal.base.controllers;

import com.personal.base.dto.address.ProvinceResponse;
import com.personal.base.dto.address.WardResponse;
import com.personal.base.services.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/provinces")
public class AddressController {

  @Autowired
  private AddressService addressService;

  @GetMapping
  public ResponseEntity<List<ProvinceResponse>> listProvinces() {
    return ResponseEntity.ok(addressService.listProvinces());
  }

  @GetMapping("/{code}/wards")
  public ResponseEntity<List<WardResponse>> listWards(@PathVariable String code) {
    return ResponseEntity.ok(addressService.listWardsByProvince(code));
  }
}
