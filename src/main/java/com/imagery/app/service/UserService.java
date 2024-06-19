package com.imagery.app.service;

import com.imagery.app.ImageryApplication;
import com.imagery.app.model.*;
import com.imagery.app.model.Collection;
import io.jsonwebtoken.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;


@Service
public class UserService {
    private final String SECRET_KEY = "oGHEhhcoGHEhhc8XFO4oGHEhhc8XFO48kuz8kuz8XFO48oGHEhhc8XFO48kuzkuz";

    private String getJwtCookie(String userName, Duration cookieMaxAge) {
        return ResponseCookie.from("imagery-jwt", Jwts.builder()
                        .subject(userName)
                        .issuedAt(new Date(System.currentTimeMillis()))
                        .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15))
                        .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                        .compact())
                .maxAge(cookieMaxAge)
                .sameSite("None")
                .secure(true)
                .httpOnly(true)
                .path("/")
                .build()
                .toString();
    }

    public ResponseEntity<String> register(User user) throws SQLException, ClassNotFoundException {
        if (new DataBaseService().userExist(user.getUserName())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Имя пользователя занято");
        }
        DataBaseService dataBaseService = new DataBaseService();
        dataBaseService.insert("Пользователь",
                new String[]{"Имя_пользователя", "Пароль", "Имя", "Фамилия", "Отчество", "Почта", "Описание", "Дата_регистрации"},
                new String[][]{new String[]{user.getUserName(), DigestUtils.sha256Hex(user.getPassword()),
                        user.getName(), user.getSecondName(), user.getPatronymic(), user.getEmail(), user.getDescription(), LocalDate.now().toString()}});
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, getJwtCookie(user.getUserName(), Duration.ofMinutes(15)))
                .body(user.getUserName());
    }

    public ResponseEntity<?> login(User user, String imageryJwt) throws SQLException, ClassNotFoundException {
        if (imageryJwt != null)
            try {
                String userName = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getPayload().getSubject();
                DataBaseService dataBaseService = new DataBaseService();
                String[] imagesCollectionsCount = dataBaseService.selectImagesCollectionsCountByUserName(userName);
                return ResponseEntity
                        .ok()
                        .body(new HashMap<String, String>() {{
                            put("userName", userName);
                            put("photoLink", dataBaseService.selectUserByUserName(userName).getPhotoLink());
                            put("imagesCount", imagesCollectionsCount[0]);
                            put("publicCollectionsCount", imagesCollectionsCount[1]);
                            put("privateCollectionsCount", imagesCollectionsCount[2]);
                        }});
            } catch (JwtException e) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.SET_COOKIE, getJwtCookie("", Duration.ofMinutes(0)))
                        .body("Пользователь не авторизован");
            }
        String q = String.format("select count(*) " +
                "from Пользователь " +
                "where Имя_пользователя = '%s' " +
                "and Пароль = '%s'", user.getUserName(), DigestUtils.sha256Hex(user.getPassword()));
        Connection connection = ImageryApplication.getConnection();
        Statement s = connection.createStatement();
        ResultSet r = s.executeQuery(q);
        r.next();
        boolean success = r.getInt(1) != 0;
        r.close();
        s.close();
        connection.close();
        if (success) {
            DataBaseService dataBaseService = new DataBaseService();
            String[] imagesCollectionsCount = dataBaseService.selectImagesCollectionsCountByUserName(user.getUserName());

            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.SET_COOKIE, getJwtCookie(user.getUserName(), Duration.ofMinutes(15)))
                    .body(new HashMap<String, String>() {{
                        put("userName", user.getUserName());
                        put("imagesCount", imagesCollectionsCount[0]);
                        put("publicCollectionsCount", imagesCollectionsCount[1]);
                        put("privateCollectionsCount", imagesCollectionsCount[2]);
                    }});
        } else return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Неверный логин или пароль");
    }

    public ResponseEntity<String> logout(String imageryJwt) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).build()
                    .parseClaimsJws(imageryJwt).getBody().getSubject();
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                            getJwtCookie("", Duration.ofMinutes(0)))
                    .body("Выход из аккаунта");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Пользователь не авторизован");
        }
    }

    public ResponseEntity<?> getProfile(String userName) throws SQLException, ClassNotFoundException {
        User user = new DataBaseService().selectUserByUserName(userName);
        return user == null
                ? ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Пользователя не существует")
                : ResponseEntity.ok().body(user);
    }

    public ResponseEntity<?> getProfileImages(String userName, String imageryJwt) throws SQLException, ClassNotFoundException, IOException {
        boolean alienProfile = true;
        if (imageryJwt != null)
            try {
                alienProfile = !userName.equals(Jwts.parser().setSigningKey(SECRET_KEY)
                        .build().parseClaimsJws(imageryJwt).getPayload().getSubject());
            } catch (JwtException ignored) {
            }
        ArrayList<CardLocal> cards = new DataBaseService()
                .selectCardsByUserName(userName, alienProfile);
        return cards == null
                ? ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Изображений не найдено")
                : ResponseEntity.ok().body(cards);
    }

    public ResponseEntity<?> getProfileCollections(String userName, String imageryJwt) throws SQLException, ClassNotFoundException {
        boolean alienProfile = true;
        if (imageryJwt != null)
            try {
                alienProfile = !userName.equals(Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getPayload().getSubject());
            } catch (JwtException ignored) {
            }
        ArrayList<Collection> collections = new DataBaseService().selectCollectionsByUserName(userName, alienProfile);
        if (collections == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Пользователя не существует");
        } else {
            HashMap<String, Object> response = new HashMap<>();
            response.put("collections", collections);
            response.put("alienProfile", alienProfile);
            return ResponseEntity.ok().body(response);
        }
    }

    public ResponseEntity<String[]> getProfileMediaAmounts(String userName, String imageryJwt) throws SQLException, ClassNotFoundException {
        String[] amount = new DataBaseService().selectImagesCollectionsCountByUserName(userName);
        if (imageryJwt != null)
            try {
                if (!userName.equals(Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getPayload().getSubject())) {
                    amount[2] = "0";
                }
            } catch (JwtException e) {
                amount[2] = "0";
            }
        else {
            amount[2] = "0";
        }
        return ResponseEntity.ok().body(amount);
    }

    public ResponseEntity<?> getProfileSettings(String imageryJwt) throws SQLException, ClassNotFoundException {
        try {
            String userName = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getBody().getSubject();
            return new DataBaseService().userExist(userName)
                    ? ResponseEntity.ok().body(new DataBaseService().selectUserByUserName(userName))
                    : ResponseEntity.badRequest().body("Пользователя не существует");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованны");
        }
    }

    public ResponseEntity<String> checkAuth(String imageryJwt) throws SQLException, ClassNotFoundException {
        try {
            return new DataBaseService().userExist(Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(imageryJwt)
                    .getBody().getSubject())
                    ? ResponseEntity.ok()
                    .body("Вы авторизованны")
                    : ResponseEntity.badRequest()
                    .body("Пользователя не существует");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Вы не авторизованны");
        }
    }

    public ResponseEntity<String> editProfile(User newUser, String imageryJwt) throws SQLException, ClassNotFoundException {
        try {
            String currentUserName = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getBody().getSubject();
            DataBaseService dataBaseService = new DataBaseService();
            if (dataBaseService.userExist(newUser.getUserName()) &&
                    dataBaseService.selectUserByUserName(currentUserName).getId() != newUser.getId())
                return ResponseEntity.badRequest().body("Имя пользователя занято");
            if (dataBaseService.userExist(currentUserName)) {
                new DataBaseService().updateUser(newUser);
                if (!newUser.getUserName().equals(currentUserName)) {
                    Date prevJwtExpiresTime = Jwts.parser().setSigningKey(SECRET_KEY).build()
                            .parseClaimsJws(imageryJwt).getBody().getExpiration();
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, ResponseCookie.from("imagery-jwt", Jwts.builder()
                                            .subject(newUser.getUserName())
                                            .issuedAt(new Date(System.currentTimeMillis()))
                                            .expiration(prevJwtExpiresTime)
                                            .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                                            .compact())
                                    .maxAge(prevJwtExpiresTime.getTime() - System.currentTimeMillis())
                                    .sameSite("None")
                                    .secure(true)
                                    .httpOnly(true)
                                    .path("/")
                                    .build()
                                    .toString())
                            .body("Профиль обновлен, произведена повторная авторизация");
                } else return ResponseEntity.ok().body("Профиль обновлен");
            } else return ResponseEntity.badRequest().body("Пользователя не существует");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователя не существует");
        }
    }

    public ResponseEntity<String> changePassword(String prevPassword, String newPassword, String imageryJwt) throws SQLException, ClassNotFoundException {
        try {
            String userName = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getBody().getSubject();
            if (new DataBaseService().userExist(userName))
                if (new DataBaseService().updateUserPassword(DigestUtils.sha256Hex(prevPassword), DigestUtils.sha256Hex(newPassword), userName))
                    return ResponseEntity.ok().body("Пароль изменен");
                else return ResponseEntity.badRequest().body("Неверный пароль");
            else return ResponseEntity.badRequest().body("Пользователя не существует");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователя не существует");
        }
    }

    public ResponseEntity<String> deleteProfile(String password, String imageryJwt) throws SQLException, ClassNotFoundException {
        try {
            String userName = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getBody().getSubject();
            if (new DataBaseService().userExist(userName))
                if (new DataBaseService().deleteUser(DigestUtils.sha256Hex(password), userName))
                    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, getJwtCookie("", Duration.ofMinutes(0))).body("Профиль удален");
                else return ResponseEntity.badRequest().body("Неверный пароль");
            else return ResponseEntity.badRequest().body("Пользователя не существует");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователя не существует");
        }
    }

    public ResponseEntity<String> addImages(ArrayList<MultipartFile> imageFiles, ArrayList<ArrayList<String>> tags, String[] descriptions, String imageryJwt) throws SQLException, ClassNotFoundException {
        try {
            String userName = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getBody().getSubject();
            DataBaseService dataBaseService = new DataBaseService();
            if (dataBaseService.userExist(userName)) {
                String[][] imageValues = new String[imageFiles.size()][6];
                int userId = dataBaseService.selectUserByUserName(userName).getId();
                int imageId = dataBaseService.selectNextImageIdByUserId(userId);
                int fileIndex = 0;
                for (MultipartFile imageFile : imageFiles) {
                    imageValues[imageId % imageFiles.size()] = new String[]{
                            String.valueOf(userId) + String.valueOf(imageId),
                            String.format("profile-%d/image-%d.%s", userId, imageId, imageFile.getOriginalFilename().substring(imageFile.getOriginalFilename().lastIndexOf(".") + 1)),
                            descriptions[fileIndex],
                            LocalDate.now().toString(),
                            LocalDate.now().toString(),
                            String.valueOf(userId)};
                    File file = new File(String.format("E://images/profile-images/profile-%d/image-%d.%s",
                            userId, imageId, imageFile.getOriginalFilename().substring(imageFile.getOriginalFilename().lastIndexOf(".") + 1)));
                    if (!file.getParentFile().exists())
                        file.getParentFile().mkdirs();
                    file.createNewFile();
                    Files.write(file.toPath(), imageFile.getBytes());
                    imageId++;
                    fileIndex++;
                }
                String userIdString = String.valueOf(userId);
                ArrayList<String[]> tagValues = new ArrayList<>();
                ArrayList<String[]> tagListValues = new ArrayList<>();
                for (int i = 0; i < tags.size(); i++) {
                    tags.set(i, new ArrayList<>(Arrays.asList(tags.get(i).toString().replaceAll("\\[|\\]", "").split(","))));
                    for (int j = 0; j < tags.get(i).size(); j++) {
                        String[] tagValue = new String[]{(userIdString + String.valueOf(imageValues[i][0]) + j), tags.get(i).get(j)};
                        String[] tagListValue = new String[]{String.valueOf(imageValues[i][0]), userIdString + String.valueOf(imageValues[i][0]) + j};
                        tagValues.add(tagValue);
                        tagListValues.add(tagListValue);
                    }
                }
                String[][] tagInsertValues = new String[tagValues.size()][];
                String[][] tagListInsertValues = new String[tagValues.size()][];
                for (int i = 0; i < tagValues.size(); i++) {
                    tagInsertValues[i] = tagValues.get(i);
                    tagListInsertValues[i] = tagListValues.get(i);
                }
                dataBaseService.insert("Изображение", new String[]{"Номер", "Ссылка", "Описание", "Дата_публикации", "Дата_последнего_изменения", "Номер_пользователя"}, imageValues);
                dataBaseService.insert("Тег", new String[]{"Номер", "Название"}, tagInsertValues);
                dataBaseService.insert("Список_тегов", new String[]{"Номер_изображения", "Номер_тега"}, tagListInsertValues);

                return ResponseEntity.ok().body("Изображение(-я) успешно добавлены");
            } else return ResponseEntity.badRequest().body("Пользователя не существует");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Изображение(-я) не были добавлены");
        }
    }

    public ResponseEntity<String> deleteImage(final String imageId, final String imageryJwt) throws SQLException, ClassNotFoundException {
        try {
            String userName = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getBody().getSubject();
            DataBaseService dataBaseService = new DataBaseService();
            if (dataBaseService.userExist(userName)) {
                if (new File(String.format("E://images/profile-images/%s", dataBaseService.selectImageByImageId(imageId).getLink())).delete())
                    new DataBaseService().deleteImageByImageId(imageId);
                return ResponseEntity.ok().body("Изображение успешно удалено");
            } else return ResponseEntity.badRequest().body("Пользователя не существует");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы");
        }
    }

    public ResponseEntity<String> deleteCollection(final String collectionId) throws SQLException, ClassNotFoundException {
        new DataBaseService().deleteCollectionByCollectionId(collectionId);
        return ResponseEntity.ok().body("Коллекция удалена");
    }

    public ResponseEntity<?> getProfileCollectionList(final String imageId, final String imageryJwt) throws SQLException, ClassNotFoundException {
        try {
            String userName = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getBody().getSubject();
            DataBaseService dataBaseService = new DataBaseService();
            if (dataBaseService.userExist(userName)) {
                return ResponseEntity.ok().body(dataBaseService.selectCollectionByImageIdUserName(imageId, userName));
            } else return ResponseEntity.badRequest().body("Пользователя не существует");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы");
        }
    }

    public ResponseEntity<?> addCollection(final String imageId, final String imageryJwt) throws SQLException, ClassNotFoundException {
        try {
            String userName = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getBody().getSubject();
            DataBaseService dataBaseService = new DataBaseService();
            if (dataBaseService.userExist(userName)) {
                User user = dataBaseService.selectUserByUserName(userName);
                String[] collectionCount = dataBaseService.selectImagesCollectionsCountByUserName(String.valueOf(user.getId()));
                String collectionId = String.valueOf(user.getId()) + String.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("mmssSSS")));
                dataBaseService.insert("Коллекция",
                        new String[]{"Номер", "Название", "Описание", "Номер_пользователя", "Номер_типа", "Дата_создания", "Дата_последнего_изменения"},
                        new String[][]{new String[]{
                                collectionId,
                                "Новая коллекция",
                                "",
                                String.valueOf(user.getId()),
                                "1",
                                LocalDate.now().toString(),
                                LocalDate.now().toString()}});
                dataBaseService.insert("Изображения_коллекции",
                        new String[]{"Номер_коллекции", "Номер_изображения"},
                        new String[][]{new String[]{collectionId, imageId}});
                return ResponseEntity.ok().body("Коллекция успешно создана");
            } else return ResponseEntity.badRequest().body("Пользователя не существует");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы");
        }
    }

    public ResponseEntity<?> addImageToCollection(final String imageId, final String collectionId, final String imageryJwt) throws SQLException, ClassNotFoundException {
        try {
            String userName = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getBody().getSubject();
            DataBaseService dataBaseService = new DataBaseService();
            if (dataBaseService.userExist(userName)) {
                dataBaseService.insert("Изображения_коллекции",
                        new String[]{"Номер_коллекции", "Номер_изображения"},
                        new String[][]{new String[]{collectionId, imageId}});
                return ResponseEntity.ok().body("Изображение успешно добавлено");
            } else return ResponseEntity.badRequest().body("Пользователя не существует");
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Вы не авторизованы");
        }
    }

    public ResponseEntity<?> getCollectionImages(final String collectionId, final String imageryJwt) throws SQLException, ClassNotFoundException, IOException {
        DataBaseService dataBaseService = new DataBaseService();
        Collection collection = dataBaseService.selectCollectionByCollectionId(String.valueOf(collectionId));
        if (collection != null) {
            ArrayList<CardLocal> response = dataBaseService.selectCardsByCollectionId(String.valueOf(collectionId));
            if (imageryJwt != null)
                try {
                    String userName = Jwts.parser().setSigningKey(SECRET_KEY).build().parseClaimsJws(imageryJwt).getBody().getSubject();
                    if (!userName.equals(response.get(0).getUser().getUserName()) && collection.getType().equals("Закрытая")) {
                        return ResponseEntity.badRequest().body("Это закрытая коллекция");
                    } else ResponseEntity.ok().body(response);
                } catch (JwtException e) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Ошибка авторизации");
                }
            if (collection.getType().equals("Закрытая"))
                return ResponseEntity.badRequest().body("Это закрытая коллекция");
            return ResponseEntity.ok().body(response);
        } else {
            return ResponseEntity.badRequest().body("Коллекции не существует");
        }
    }

    public ResponseEntity<?> getCollection(final String collectionId, final String imageryJwt) throws SQLException, ClassNotFoundException, IOException {
        DataBaseService dataBaseService = new DataBaseService();
        Collection collection = dataBaseService.selectCollectionByCollectionId(String.valueOf(collectionId));
        if (collection != null) {
            return ResponseEntity.ok().body(new DataBaseService().selectCollectionByCollectionId(collectionId));
        } else {
            return ResponseEntity.badRequest().body("Коллекции не существует");
        }
    }
}
