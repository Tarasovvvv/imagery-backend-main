package com.imagery.app.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class User {
    private int id;
    private String userName;
    private String password;
    private String name;
    private String secondName;
    private String patronymic;
    private String email;
    private String description;
    private Date createDate;
    private String photoLink;
}
