package org.example.folderservice.feign;

import org.example.folderservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for communicating with user-service.
 * Used to fetch user information for folder operations.
 */
@FeignClient(name = "user-service", url = "${services.user-service.url:http://localhost:8081}")
public interface UserServiceClient {
    
    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
    
    @GetMapping("/api/users/email/{email}")
    UserDTO getUserByEmail(@PathVariable("email") String email);
    
    @GetMapping("/api/users/exists")
    boolean userExists(@RequestParam("email") String email);
}
