package com.runner.shopping.model.request;

import lombok.Data;

@Data
public class UpdatePassword {
    public String oldPassword;
    public String newPassword;
}
