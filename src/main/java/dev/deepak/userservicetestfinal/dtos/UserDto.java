package dev.deepak.userservicetestfinal.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.deepak.userservicetestfinal.models.Role;
import dev.deepak.userservicetestfinal.models.User;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class UserDto {
    private String email;
    @JsonIgnore
    private Set<Role> roles = new HashSet<>();

    public static UserDto from(User user) {
        UserDto userDto = new UserDto();
        userDto.setEmail(user.getEmail());
        userDto.setRoles(user.getRoles());

        return userDto;
    }
}
