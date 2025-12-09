package org.example.fileservice.feign;


import org.example.commonlibrary.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);

    @GetMapping("/users/email/{email}")
    UserDTO getUserByEmail(@PathVariable("email") String email);
}
