package com.archit.profilemail.controller;

import com.archit.profilemail.dto.AuthResponse;
import com.archit.profilemail.dto.LoginRequest;
import com.archit.profilemail.dto.RegisterRequest;
import com.archit.profilemail.model.UserAccount;
import com.archit.profilemail.service.AuthService;
import com.archit.profilemail.utils.JWTUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    private final JWTUtils jwtUtils;

    public AuthController(AuthService authService, JWTUtils jwtUtils){
        this.authService=authService;
        this.jwtUtils=jwtUtils;
    }

    @GetMapping("/")
    public String home(HttpServletResponse response) {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        System.out.println("Hello Archit");
        return "Hello";
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String,AuthResponse>> register(@RequestBody RegisterRequest req) {
        //We don't use @RequestBody User user directly here because of following reasons:
        //authService.registerUser(user)
        //1. If your User entity has extra fields like id, roles, or createdAt, users might send unexpected data.
        //   Ex - End user sends data with role = Admin, which was not intended but now saved in DB
        //2. If we modify the User entity (e.g., add a new column), it could break our API because our API directly depends on the entity.
        //3. Without proper validation, a user might send incomplete data, and our application might crash.

        //Better option is to use DTO which contains the fields we need in order to register our user
        UserAccount newUserAccount = new UserAccount();
        newUserAccount.setEmail(req.getEmail());
        newUserAccount.setPassword(req.getPassword());
        String token = jwtUtils.generateToken(newUserAccount);
        authService.registerUser(newUserAccount);
        return ResponseEntity.ok(Map.of("token",new AuthResponse(token)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req){
        try{
            UserDetails user = authService.authenticate(req.getEmail(),req.getPassword());
            String token = jwtUtils.generateToken(user);
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (UsernameNotFoundException | BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse("Invalid Credentials"));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<String> user(@RequestBody String email) {
        UserAccount userAccountFromDb = authService.loadUserByUsername(email);
        if(userAccountFromDb ==null){
            return new ResponseEntity<>(HttpStatusCode.valueOf(404));
        }
        return ResponseEntity.ok(userAccountFromDb.getEmail()+","+userAccountFromDb.getId());
    }
}
