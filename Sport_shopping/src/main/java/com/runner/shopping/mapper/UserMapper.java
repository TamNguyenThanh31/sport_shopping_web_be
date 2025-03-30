package com.runner.shopping.mapper;

import com.runner.shopping.model.dto.UserDTO;
import com.runner.shopping.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    // Ánh xạ từ User sang UserDTO
    UserDTO toDTO(User user);

    // Ánh xạ từ UserDTO sang User (nếu cần)
    User toEntity(UserDTO userDTO);

    // Ánh xạ danh sách
    Iterable<UserDTO> toDTOList(Iterable<User> users);
}
