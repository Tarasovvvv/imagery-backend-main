package com.imagery.app.controller;

import com.imagery.app.model.User;
import com.imagery.app.service.TagExtractorService;
import com.imagery.app.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@CrossOrigin(origins = {"https://192.168.1.45:3000",
        "https://192.168.1.43",
        "https://192.168.1.*"},
        allowCredentials = "true")
@RestController
@RequestMapping("/api")
public class UserController {

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody User user) throws SQLException, ClassNotFoundException {
        return new UserService().register(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestBody(required = false) User user) throws SQLException, ClassNotFoundException {
        if (imageryJwt == null)
            if (user.getUserName() == null || user.getPassword() == null)
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Для входа требуется ввести логин и пароль!");
        return new UserService().login(user, imageryJwt);
    }

    @GetMapping("/logout")
    public ResponseEntity<String> logout(
            @CookieValue(value = "imagery-jwt") String imageryJwt) {
        return new UserService().logout(imageryJwt);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @RequestParam String userName) throws SQLException, ClassNotFoundException {
        return new UserService().getProfile(userName);
    }

    @GetMapping("/profile-media-amounts")
    public ResponseEntity<?> getProfileMediaAmounts(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestParam String userName) throws SQLException, ClassNotFoundException {
        return new UserService().getProfileMediaAmounts(userName, imageryJwt);
    }

    @GetMapping("/profile-images")
    public ResponseEntity<?> getProfileImages(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestParam String userName) throws SQLException, ClassNotFoundException, IOException {
        return new UserService().getProfileImages(userName, imageryJwt);
    }

    @GetMapping("/profile-collections")
    public ResponseEntity<?> getProfileCollections(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestParam String userName) throws SQLException, ClassNotFoundException {
        return new UserService().getProfileCollections(userName, imageryJwt);
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getProfileSettings(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt)
            throws SQLException, ClassNotFoundException {
        return imageryJwt == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы")
                : new UserService().getProfileSettings(imageryJwt);
    }

    @GetMapping("/check-auth")
    public ResponseEntity<?> checkAuth(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt)
            throws SQLException, ClassNotFoundException {
        return imageryJwt == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы")
                : new UserService().checkAuth(imageryJwt);
    }

    @PatchMapping("/edit-profile")
    public ResponseEntity<?> editProfile(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestBody User user) throws SQLException, ClassNotFoundException {
        return imageryJwt == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы")
                : new UserService().editProfile(user, imageryJwt);
    }

    @PatchMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestBody HashMap<String, String> passwords) throws SQLException, ClassNotFoundException {
        return imageryJwt == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы")
                : new UserService().changePassword(passwords.get("prevPassword"),
                passwords.get("newPassword"), imageryJwt);
    }

    @DeleteMapping("/delete-profile")
    public ResponseEntity<String> deleteProfile(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestBody String password) throws SQLException, ClassNotFoundException {
        return imageryJwt == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы")
                : new UserService().deleteProfile(password, imageryJwt);
    }

    @PostMapping("/add-image")
    public ResponseEntity<String> addImages(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestParam("files[]") ArrayList<MultipartFile> imageFiles,
            @RequestParam("tags[]") String[] tags,
            @RequestParam("descriptions[]") String[] descriptions)
            throws SQLException, ClassNotFoundException {
        return imageryJwt == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы")
                : new UserService().addImages(imageFiles,
                new TagExtractorService().getTags(tags), descriptions, imageryJwt);
    }

    @DeleteMapping("/delete-image")
    public ResponseEntity<String> deleteImage(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestBody final String imageId) throws SQLException, ClassNotFoundException {
        return imageryJwt == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы")
                : new UserService().deleteImage(imageId, imageryJwt);
    }

    @DeleteMapping("/delete-collection")
    public ResponseEntity<String> deleteCollection(
            @RequestBody final String imageId) throws SQLException, ClassNotFoundException {
        return new UserService().deleteCollection(imageId);
    }

    @GetMapping("/collection-images")
    public ResponseEntity<?> getCollectionImages(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestParam(name = "collectionId") String collectionId)
            throws SQLException, ClassNotFoundException, IOException {
        return new UserService().getCollectionImages(collectionId, imageryJwt);
    }

    @GetMapping("/collection")
    public ResponseEntity<?> getCollection(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestParam(name = "id") String id)
            throws SQLException, ClassNotFoundException, IOException {
        return new UserService().getCollection(id, imageryJwt);
    }

    @PostMapping("/profile-collection-list")
    public ResponseEntity<?> getProfileCollectionList(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestBody final String imageId) throws SQLException, ClassNotFoundException {
        return imageryJwt == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы")
                : new UserService().getProfileCollectionList(imageId, imageryJwt);
    }

    @PostMapping("/add-collection")
    public ResponseEntity<?> addCollection(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestBody final String imageId) throws SQLException, ClassNotFoundException {
        return imageryJwt == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы")
                : new UserService().addCollection(imageId, imageryJwt);
    }

    @PostMapping("/add-image-to-collection")
    public ResponseEntity<?> addImageToCollection(
            @CookieValue(value = "imagery-jwt", required = false) String imageryJwt,
            @RequestBody final String request) throws SQLException, ClassNotFoundException {
        final String[] s = request.substring(1, request.length() - 1).split(" ");
        String imageId = s[0];
        String collectionId = s[1];
        return imageryJwt == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы")
                : new UserService().addImageToCollection(imageId, collectionId, imageryJwt);
    }
}
