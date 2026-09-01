package com.personal.base.services;

import com.personal.base.dto.address.ProvinceResponse;
import com.personal.base.dto.address.WardResponse;
import com.personal.base.repository.ProvinceRepository;
import com.personal.base.repository.WardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressService {

  @Autowired
  private ProvinceRepository provinceRepository;

  @Autowired
  private WardRepository wardRepository;

  @Cacheable("provinces")
  public List<ProvinceResponse> listProvinces() {
    return provinceRepository.findAll(Sort.by("name")).stream()
            .map(ProvinceResponse::from)
            .collect(Collectors.toList());
  }

  public List<WardResponse> listWardsByProvince(String provinceCode) {
    return wardRepository.findByProvinceCode(provinceCode).stream()
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .map(WardResponse::from)
            .collect(Collectors.toList());
  }

  @Cacheable("wards")
  public List<WardResponse> listAllWards() {
    return wardRepository.findAll(Sort.by("name")).stream()
            .map(WardResponse::from)
            .collect(Collectors.toList());
  }
}
