package com.carbonpulse.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carbonpulse.entity.BehaviorType;
import com.carbonpulse.mapper.BehaviorTypeMapper;
import com.carbonpulse.service.BehaviorTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BehaviorTypeServiceImpl extends ServiceImpl<BehaviorTypeMapper, BehaviorType> implements BehaviorTypeService {
    @Override
    public List<BehaviorType> findAll() {
        return list();
    }

    @Override
    public BehaviorType findById(Long id) {
        return getById(id);
    }


    @Override
    public void deleteById(Long id) {
        removeById(id);
    }
}
