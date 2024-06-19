package com.imagery.app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ImageLocal {
    private int id;
    private String link;
    private String description;
    private Date createDate;
    private Date lastEditDate;
    private int userId;
    private byte[] imageFile;
}
