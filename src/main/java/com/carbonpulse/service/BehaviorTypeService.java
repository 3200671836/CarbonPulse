package com.carbonpulse.service;

import com.carbonpulse.entity.BehaviorType;

import java.util.List;

public interface BehaviorTypeService {
    List<BehaviorType> findAll();
    BehaviorType findById(Long id);
    void deleteById(Long id);
}
