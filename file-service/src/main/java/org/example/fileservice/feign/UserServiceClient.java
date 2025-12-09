package org.example.fileservice.feign;

import org.example.fileservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", path = "/api/users")
public interface UserServiceClient {
    
    @GetMapping("/profile/{userId}")
    UserDTO getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/public/email/{email}")
    UserDTO getUserByEmail(@PathVariable("email") String email);
    
    @PutMapping("/internal/{userId}/storage")
    void updateStorageUsed(@PathVariable("userId") Long userId, @RequestParam("sizeChange") Long sizeChange);
    
    @GetMapping("/internal/{userId}/storage/check")
    Boolean hasStorageSpace(@PathVariable("userId") Long userId, @RequestParam("fileSize") Long fileSize);
}
