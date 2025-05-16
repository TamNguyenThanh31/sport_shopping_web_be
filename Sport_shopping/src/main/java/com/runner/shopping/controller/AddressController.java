package com.runner.shopping.controller;

import com.runner.shopping.model.dto.AddressDTO;
import com.runner.shopping.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressDTO> createAddress(@RequestParam Long userId, @Valid @RequestBody AddressDTO addressDTO) {
        AddressDTO createdAddress = addressService.createAddress(userId, addressDTO);
        return ResponseEntity.ok(createdAddress);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(@RequestParam Long userId, @PathVariable Long addressId, @Valid @RequestBody AddressDTO addressDTO) {
        AddressDTO updatedAddress = addressService.updateAddress(userId, addressId, addressDTO);
        return ResponseEntity.ok(updatedAddress);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@RequestParam Long userId, @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressDTO> getAddressById(@RequestParam Long userId, @PathVariable Long addressId) {
        AddressDTO addressDTO = addressService.getAddressById(userId, addressId);
        return ResponseEntity.ok(addressDTO);
    }

    @GetMapping
    public ResponseEntity<List<AddressDTO>> getAddressesByUserId(@RequestParam Long userId) {
        List<AddressDTO> addresses = addressService.getAddressesByUserId(userId);
        return ResponseEntity.ok(addresses);
    }
}
