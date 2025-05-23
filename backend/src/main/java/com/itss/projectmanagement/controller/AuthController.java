package com.itss.projectmanagement.controller;

import com.itss.projectmanagement.dto.request.auth.AuthRequest;
import com.itss.projectmanagement.dto.request.auth.RegisterRequest;
import com.itss.projectmanagement.dto.response.auth.AuthResponse;
import com.itss.projectmanagement.dto.response.auth.RegisterResponse;
import com.itss.projectmanagement.entity.User;
import com.itss.projectmanagement.security.JwtTokenProvider;
import com.itss.projectmanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Operation(summary = "Authenticate user", description = "Login with username and password to get JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@RequestBody AuthRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken((User) authentication.getPrincipal());
        
        // Update last login timestamp
        userService.updateLastLogin(loginRequest.getUsername());

        return ResponseEntity.ok(new AuthResponse(jwt));
    }
    
    @Operation(summary = "Register a new user", description = "Register a new user with default STUDENT role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or username/email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User user = userService.register(
                    registerRequest.getUsername(),
                    registerRequest.getPassword(),
                    registerRequest.getFullName(),
                    registerRequest.getEmail()
            );
            
            RegisterResponse response = new RegisterResponse(
                    "User registered successfully",
                    true,
                    user.getUsername()
            );
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            RegisterResponse response = new RegisterResponse(
                    e.getMessage(),
                    false,
                    null
            );
            
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}