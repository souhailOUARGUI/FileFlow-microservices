package org.example.userservice.mapper;

import org.example.userservice.dto.UserDTO;
import org.example.userservice.dto.UserResponseDTO;
import org.example.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "fullName", source = ".", qualifiedByName = "toFullName")
    @Mapping(target = "storageUsedPercentage", source = ".", qualifiedByName = "calculateStoragePercentage")
    UserResponseDTO toResponseDTO(User user);

    @Mapping(target = "fullName", source = ".", qualifiedByName = "toFullName")
    UserDTO toDTO(User user);

    @Named("toFullName")
    default String toFullName(User user) {
        if (user == null) return null;
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }

    @Named("calculateStoragePercentage")
    default Double calculateStoragePercentage(User user) {
        if (user == null || user.getMaxStorage() == 0) return 0.0;
        return ((double) user.getStorageUsed() / (double) user.getMaxStorage()) * 100;
    }
}
