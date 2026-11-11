package vn.datve.dat_ve.auth.dto;

import vn.datve.dat_ve.auth.Role;
import vn.datve.dat_ve.auth.User;

public record UserResponse(Long id, String email, String fullName, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
