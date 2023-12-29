package dev.deepak.userservicetestfinal.services;

import dev.deepak.userservicetestfinal.dtos.UserDto;
import dev.deepak.userservicetestfinal.models.Role;
import dev.deepak.userservicetestfinal.models.SessionStatus;
import dev.deepak.userservicetestfinal.models.User;
import dev.deepak.userservicetestfinal.models.Session;
import dev.deepak.userservicetestfinal.repositories.SessionRepository;
import dev.deepak.userservicetestfinal.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.MacAlgorithm;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMapAdapter;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AuthService {
    private UserRepository userRepository;
    private SessionRepository sessionRepository;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public AuthService(UserRepository userRepository, SessionRepository sessionRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public ResponseEntity<UserDto> login(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            return null;
        }

        User user = userOptional.get();

        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            //throw an exception
            throw new RuntimeException("Wrong password entered");
        }

        //Generating the token
        //String token = RandomStringUtils.randomAlphanumeric(30);

        // Create a test key suitable for the desired HMAC-SHA algorithm:
        MacAlgorithm alg = Jwts.SIG.HS256; //or HS384 or HS256
        SecretKey key = alg.key().build();

//        String message = "{\n" +
//                "  \"email\": \"harsh@scaler.com\",\n" +
//                "  \"roles\": [\n" +
//                "    \"student\",\n" +
//                "    \"ta\"\n" +
//                "  ],\n" +
//                "  \"expiry\": \"31stJan2024\"\n" +
//                "}";
        //JSON -> Key : Value
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("email", user.getEmail());
        jsonMap.put("roles", List.of(user.getRoles()));
        jsonMap.put("createdAt", new Date());
        jsonMap.put("expiryAt", DateUtils.addDays(new Date(), 30));

        //byte[] content = message.getBytes(StandardCharsets.UTF_8);

        // Create the compact JWS:
        //String jws = Jwts.builder().content(content, "text/plain").signWith(key, alg).compact();
        String jws = Jwts.builder()
                .claims(jsonMap)
                .signWith(key, alg)
                .compact();

        // Parse the compact JWS:
        //content = Jwts.parser().verifyWith(key).build().parseSignedContent(jws).getPayload();
        //assert message.equals(new String(content, StandardCharsets.UTF_8));

        Session session = new Session();
        session.setSessionStatus(SessionStatus.ACTIVE);
        session.setToken(jws);
        session.setUser(user);
        //session.setExpiringAt(//current time + 30 days);
        sessionRepository.save(session);

        UserDto userDto = new UserDto();
        userDto.setEmail(email);

        MultiValueMapAdapter<String, String> headers = new MultiValueMapAdapter<>(new HashMap<>());
        headers.add(HttpHeaders.SET_COOKIE, "auth-token:" + jws);

        ResponseEntity<UserDto> response = new ResponseEntity<>(userDto, headers, HttpStatus.OK);

        return response;
    }

    public ResponseEntity<Void> logout(String token, Long userId) {
        Optional<Session> sessionOptional = sessionRepository.findByTokenAndUser_Id(token, userId);

        if (sessionOptional.isEmpty()) {
            return null;
        }

        Session session = sessionOptional.get();

        session.setSessionStatus(SessionStatus.ENDED);

        sessionRepository.save(session);

        return ResponseEntity.ok().build();
    }

    public UserDto signUp(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(bCryptPasswordEncoder.encode(password)); // We should store the encrypted password in the DB for a user.
        
        User savedUser = userRepository.save(user);

        return UserDto.from(savedUser);
    }

    public SessionStatus validate(String token, Long userId) {
        Optional<Session> sessionOptional = sessionRepository.findByTokenAndUser_Id(token, userId);

        if (sessionOptional.isEmpty()) {
            return null;
        }

        Session session = sessionOptional.get();

        if (!session.getSessionStatus().equals(SessionStatus.ACTIVE)) {
            return SessionStatus.ENDED;
        }

        Date currentTime = new Date();
        if (session.getExpiringAt().before(currentTime)) {
            return SessionStatus.ENDED;
        }

        //JWT Decoding.
        Jws<Claims> jwsClaims = Jwts.parser().build().parseSignedClaims(token);

        // Map<String, Object> -> Payload object or JSON
        String email = (String) jwsClaims.getPayload().get("email");
        List<Role> roles = (List<Role>) jwsClaims.getPayload().get("roles");
        Date createdAt = (Date) jwsClaims.getPayload().get("createdAt");

//        if (restrictedEmails.contains(email)) {
//            //reject the token
//        }

        return SessionStatus.ACTIVE;
    }
}

/*

eyJjdHkiOiJ0ZXh0L3BsYWluIiwiYWxnIjoiSFMyNTYifQ.
SGVsbG8gV29ybGQh.
EHQJBVvni4oDe_NEqnecIwNmOTUe_7Hs_jVW_XT-b1o

*/

/*
Task-1 : Implement limit on number of active sessions for a user.
Task-2 : Implement login workflow using the token details with validation of expiry date.
 */
