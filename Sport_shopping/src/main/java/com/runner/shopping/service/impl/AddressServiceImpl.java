package com.runner.shopping.service.impl;

import com.runner.shopping.entity.Addresses;
import com.runner.shopping.enums.UserRole;
import com.runner.shopping.exception.ResourceNotFoundException;
import com.runner.shopping.mapper.AddressMapper;
import com.runner.shopping.model.dto.AddressDTO;
import com.runner.shopping.repository.AddressRepository;
import com.runner.shopping.repository.OrderRepository;
import com.runner.shopping.repository.UserRepository;
import com.runner.shopping.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public AddressDTO createAddress(Long userId, AddressDTO addressDTO) {
        validateUser(userId);
        Addresses address = addressMapper.toEntity(addressDTO);
        address.setUserId(userId);
        if (addressDTO.getIsDefault() != null && addressDTO.getIsDefault()) {
            addressRepository.findAll().stream()
                    .filter(a -> a.getUserId().equals(userId) && a.getIsDefault())
                    .forEach(a -> {
                        a.setIsDefault(false);
                        addressRepository.save(a);
                    });
        }
        Addresses savedAddress = addressRepository.save(address);
        return addressMapper.toDTO(savedAddress);
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long userId, Long addressId, AddressDTO addressDTO) {
        validateUser(userId);
        Addresses address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId + " for user: " + userId));
        address.setFullName(addressDTO.getFullName());
        address.setPhone(addressDTO.getPhone());
        address.setAddressLine(addressDTO.getAddressLine());
        address.setCity(addressDTO.getCity());
        address.setCountry(addressDTO.getCountry());
        if (addressDTO.getIsDefault() != null && addressDTO.getIsDefault()) {
            addressRepository.findAll().stream()
                    .filter(a -> a.getUserId().equals(userId) && a.getIsDefault() && !a.getId().equals(addressId))
                    .forEach(a -> {
                        a.setIsDefault(false);
                        addressRepository.save(a);
                    });
            address.setIsDefault(true);
        }
        Addresses updatedAddress = addressRepository.save(address);
        return addressMapper.toDTO(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        validateUser(userId);
        Addresses address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId + " for user: " + userId));
        if (orderRepository.existsByAddressId(addressId)) {
            throw new IllegalStateException("Cannot delete address used in orders");
        }
        addressRepository.delete(address);
    }

    @Override
    public AddressDTO getAddressById(Long userId, Long addressId) {
        validateUser(userId);
        Addresses address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId + " for user: " + userId));
        return addressMapper.toDTO(address);
    }

    @Override
    public List<AddressDTO> getAddressesByUserId(Long userId) {
        validateUser(userId);
        List<Addresses> addresses = addressRepository.findAll().stream()
                .filter(a -> a.getUserId().equals(userId))
                .collect(Collectors.toList());
        return addressMapper.toDTOList(addresses);
    }

    private void validateUser(Long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or not a customer with id: " + userId));
    }
}
