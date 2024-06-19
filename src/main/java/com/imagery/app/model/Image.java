package com.imagery.app.model;

import lombok.*;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Image {
    private int id;
    private String link;
    private String description;
    private Date createDate;
    private Date lastEditDate;
    private int userId;
}
