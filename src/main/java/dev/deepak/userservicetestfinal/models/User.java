package dev.deepak.userservicetestfinal.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.springframework.data.repository.cdi.Eager;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@JsonDeserialize(as = User.class)
public class User extends BaseModel {
    private String email;
    private String password;

    @ManyToMany(fetch = jakarta.persistence.FetchType.EAGER)
    @JsonIgnore
    private Set<Role> roles = new HashSet<>();
}
