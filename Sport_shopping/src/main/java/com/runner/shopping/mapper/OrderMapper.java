package com.runner.shopping.mapper;

import com.runner.shopping.entity.OrderDetails;
import com.runner.shopping.entity.Orders;
import com.runner.shopping.model.dto.OrderDTO;
import com.runner.shopping.model.dto.OrderDetailDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)   // Service sẽ gán
    @Mapping(target = "totalCost", ignore = true)    // Service sẽ gán
    @Mapping(target = "totalProfit", ignore = true)  // Service sẽ gán
    @Mapping(target = "canceledAt", ignore = true)   // Service gán khi hủy đơn
    Orders toEntity(OrderDTO orderDTO);

    // Map Orders → OrderDTO (MapStruct tự động map các trường không ignore)
    OrderDTO toDTO(Orders orders);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderDetails toEntity(OrderDetailDTO orderDetailDTO);

    OrderDetailDTO toDTO(OrderDetails orderDetails);

    List<OrderDTO> toDTOList(List<Orders> orders);

    OrderDetailDTO toDetailDTO(OrderDetails orderDetail);
}
