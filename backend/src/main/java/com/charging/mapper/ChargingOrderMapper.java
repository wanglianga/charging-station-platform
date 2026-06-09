package com.charging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.charging.entity.ChargingOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChargingOrderMapper extends BaseMapper<ChargingOrder> {
}
