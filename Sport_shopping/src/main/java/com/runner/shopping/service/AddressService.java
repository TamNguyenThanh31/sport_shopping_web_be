package com.runner.shopping.service;

import com.runner.shopping.model.dto.AddressDTO;

import java.util.List;

public interface AddressService {

    AddressDTO createAddress(Long userId, AddressDTO addressDTO);

    AddressDTO updateAddress(Long userId, Long addressId, AddressDTO addressDTO);

    void deleteAddress(Long userId, Long addressId);

    AddressDTO getAddressById(Long userId, Long addressId);

    List<AddressDTO> getAddressesByUserId(Long userId);
}
