package com.imagery.app.model;

import lombok.*;

import java.util.ArrayList;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Collection {
    private int id;
    private String name;
    private String description;
    private int userId;
    private String type;
    private Date createDate;
    private Date lastEditDate;
    private ArrayList<Image> previewImages;
}
