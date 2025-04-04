package com.runner.shopping.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    String identifier;
    String password;
}
