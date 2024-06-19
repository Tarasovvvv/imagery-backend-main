package com.imagery.app.service;

import com.imagery.app.ImageryApplication;
import com.imagery.app.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class DataBaseService {
    public boolean userExist(String userName) throws SQLException, ClassNotFoundException {
        Connection connection = ImageryApplication.getConnection();
        String q = String.format("select count(*) " +
                "from Пользователь " +
                "where Имя_пользователя = '%s' ", userName);
        Statement s = connection.createStatement();
        ResultSet r = s.executeQuery(q);
        r.next();
        boolean result = r.getInt(1) >= 1;
        r.close();
        s.close();
        connection.close();
        return result;
    }

    public void insert(final String table, final String[] columns, final String[][] values) throws SQLException, ClassNotFoundException {
        StringBuilder sb = new StringBuilder();
        for (String[] row : values) {
            sb.append("(");
            for (String value : row)
                sb.append("'").append(value).append("',");
            sb.deleteCharAt(sb.length() - 1);
            sb.append("),");
        }
        sb.deleteCharAt(sb.length() - 1);
        String query = String.format("insert into %s(%s) values %s;", table, String.join(", ", columns), sb);
        System.out.println(String.format("insert into %s(%s) values %s;", table, String.join(", ", columns), sb));


        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        statement.executeUpdate(query);
        statement.close();
        connection.close();
    }

    public ArrayList<ImageLocal> selectImagesByTagNames(final String[] tagNames) throws SQLException, ClassNotFoundException, IOException {
        String query = String.format("select distinct Изображение.* " +
                "from Изображение, Список_тегов, Тег " +
                "where Список_тегов.Номер_тега = Тег.Номер " +
                "and Изображение.Номер = Список_тегов.Номер_изображения " +
                "and Тег.Название in ('%s') " +
                "limit 30", String.join("', '", tagNames));
System.out.println(query);
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(query);

        ArrayList<ImageLocal> images = new ArrayList<>();
        while (result.next()) {
            String link = result.getString("Ссылка");
            images.add(new ImageLocal(
                    result.getInt("Номер"),
                    link,
                    result.getString("Описание"),
                    result.getDate("Дата_публикации"),
                    result.getDate("Дата_последнего_изменения"),
                    result.getInt("Номер_пользователя"),
                    link.charAt(0) == 'p' ? Files.readAllBytes(Paths.get(String.format("E://images/profile-images/%s", link))) : null));
        }
        result.close();
        statement.close();
        connection.close();
        return images;
    }

    public ArrayList<String> selectTagNamesByImageId(final int imageId) throws SQLException, ClassNotFoundException {
        String query = String.format("select Тег.Название " +
                "from Тег, Список_тегов " +
                "where Список_тегов.Номер_изображения = '%d' " +
                "and Тег.Номер = Список_тегов.Номер_тега", imageId);

        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(query);

        ArrayList<String> tagNames = new ArrayList<>();
        while (result.next())
            tagNames.add(result.getString("Название"));

        result.close();
        statement.close();
        connection.close();
        return tagNames;
    }

    public User selectUserIdNameByImageId(final int imageId) throws SQLException, ClassNotFoundException {
        String query = String.format("select Пользователь.Номер, Пользователь.Имя_пользователя, Пользователь.Ссылка_фото " +
                "from Пользователь, Изображение " +
                "where Изображение.Номер = '%d' " +
                "and Пользователь.Номер = Изображение.Номер_пользователя", imageId);
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(query);
        User user = new User();
        if (result.next()) {
            user.setId(result.getInt("Номер"));
            user.setUserName(result.getString("Имя_пользователя"));
            user.setPhotoLink(result.getString("Ссылка_фото"));
        } else {
            user.setId(-1);
            user.setUserName("Пользователь не найден");
        }
        result.close();
        statement.close();
        connection.close();
        return user;
    }

    public User selectUserByUserName(final String userName) throws SQLException, ClassNotFoundException {
        String query = String.format("select Номер, Имя_пользователя, Фамилия, Имя, Отчество, Почта, Описание, Дата_регистрации, Ссылка_фото " +
                "from Пользователь " +
                "where Имя_пользователя = '%s'", userName);
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(query);
        User user = null;
        if (result.next()) {
            user = new User();
            user.setId(result.getInt("Номер"));
            user.setUserName(result.getString("Имя_пользователя"));
            user.setSecondName(result.getString("Фамилия"));
            user.setName(result.getString("Имя"));
            user.setPatronymic(result.getString("Отчество"));
            user.setEmail(result.getString("Почта"));
            user.setDescription(result.getString("Описание"));
            user.setCreateDate(result.getDate("Дата_регистрации"));
            user.setPhotoLink(result.getString("Ссылка_фото"));
        }
        result.close();
        statement.close();
        connection.close();
        return user;
    }

    public String[] selectImagesCollectionsCountByUserName(String userName) throws SQLException, ClassNotFoundException {
        String query = String.format("select count(Изображение.*) " +
                "from Изображение, Пользователь " +
                "where Пользователь.Имя_пользователя = '%s' " +
                "and Изображение.Номер_пользователя = Пользователь.Номер", userName);
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(query);
        String[] response = new String[3];
        result.next();
        response[0] = String.valueOf(result.getInt(1));

        query = String.format("select count(Коллекция.*) " +
                "from Коллекция, Тип_коллекции, Пользователь " +
                "where Пользователь.Имя_пользователя = '%s' " +
                "and Коллекция.Номер_пользователя = Пользователь.Номер " +
                "and Тип_коллекции.Номер = Коллекция.Номер_типа " +
                "and Тип_коллекции.Название = 'Открытая'", userName);
        result = statement.executeQuery(query);
        result.next();
        response[1] = String.valueOf(result.getInt(1));

        query = String.format("select count(Коллекция.*) " +
                "from Коллекция, Тип_коллекции, Пользователь " +
                "where Пользователь.Имя_пользователя = '%s' " +
                "and Коллекция.Номер_пользователя = Пользователь.Номер " +
                "and Тип_коллекции.Номер = Коллекция.Номер_типа " +
                "and Тип_коллекции.Название = 'Закрытая'", userName);
        result = statement.executeQuery(query);
        result.next();
        response[2] = String.valueOf(result.getInt(1));

        result.close();
        statement.close();
        connection.close();
        return response;
    }

    public ArrayList<CardLocal> selectCardsByUserName(final String userName, boolean alienProfile) throws SQLException, ClassNotFoundException, IOException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(String.format("select Изображение.* " +
                "from Изображение, Пользователь " +
                "where Изображение.Номер_пользователя = Пользователь.Номер " +
                "and Пользователь.Имя_пользователя = '%s' limit 30", userName));
        User user = new User();
        user.setId(alienProfile ? -1 : 1);
        ArrayList<CardLocal> cards = new ArrayList<>();
        while (result.next()) {
            int imageId = result.getInt("Номер");
            ImageLocal image = new ImageLocal();
            image.setId(imageId);
            image.setLink(result.getString("Ссылка"));
            image.setDescription(result.getString("Описание"));
            image.setCreateDate(result.getDate("Дата_публикации"));
            image.setCreateDate(result.getDate("Дата_последнего_изменения"));
            image.setImageFile(image.getLink().charAt(0) == 'p' ? Files.readAllBytes(Paths.get(String.format("E://images/profile-images/%s", image.getLink()))) : null);
            cards.add(new CardLocal(image, selectTagNamesByImageId(imageId), user));
        }
        result.close();
        statement.close();
        connection.close();
        return cards;
    }

    public ArrayList<CardLocal> selectCardsByCollectionId(final String collectionId) throws SQLException, ClassNotFoundException, IOException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(String.format("select Изображение.* " +
                "from Изображение, Изображения_коллекции " +
                "where Изображения_коллекции.Номер_коллекции = '%s' " +
                "and Изображение.Номер = Изображения_коллекции.Номер_изображения", collectionId));
        ArrayList<CardLocal> cards = new ArrayList<>();
        while (result.next()) {
            int imageId = result.getInt("Номер");
            ImageLocal image = new ImageLocal();
            image.setId(imageId);
            image.setLink(result.getString("Ссылка"));
            image.setDescription(result.getString("Описание"));
            image.setCreateDate(result.getDate("Дата_публикации"));
            image.setCreateDate(result.getDate("Дата_последнего_изменения"));
            image.setUserId(result.getInt("Номер_пользователя"));
            image.setImageFile(image.getLink().charAt(0) == 'p' ? Files.readAllBytes(Paths.get(String.format("E://images/profile-images/%s", image.getLink()))) : null);
            cards.add(new CardLocal(image, selectTagNamesByImageId(imageId), selectUserIdNameByImageId(image.getId())));
        }
        result.close();
        statement.close();
        connection.close();
        return cards;
    }

    public Collection selectCollectionByCollectionId(final String collectionId) throws SQLException, ClassNotFoundException, IOException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(String.format("select * " +
                "from Коллекция where Номер = '%s'", collectionId));
        Collection collection = null;
        if (result.next()) {
            collection = new Collection(result.getInt("Номер"), result.getString("Название"), result.getString("Описание"), result.getInt("Номер_пользователя"),
                    result.getInt("Номер_типа") == 1 ? "Открытая" : "Закрытая", result.getDate("Дата_создания"), result.getDate("Дата_последнего_изменения"), null);
        }
        result.close();
        statement.close();
        connection.close();
        return collection;
    }

    public ArrayList<Collection> selectCollectionsByUserName(final String userName, final boolean alienProfile) throws SQLException, ClassNotFoundException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(String.format("select Коллекция.* from Коллекция, Пользователь " +
                "where Коллекция.Номер_пользователя = Пользователь.Номер " +
                "and Пользователь.Имя_пользователя = '%s'", userName));
        ArrayList<Collection> collections = new ArrayList<>();
        while (result.next()) {
            Statement statement2 = connection.createStatement();
            ResultSet result2 = statement2.executeQuery(String.format("select Название " +
                    "from Тип_коллекции where Номер = %s", result.getInt("Номер_типа")));
            result2.next();
            String collectionType = result2.getString("Название");
            if (!(alienProfile && collectionType.equals("Закрытая"))) {
                int collectionId = result.getInt("Номер");
                result2 = statement2.executeQuery(String.format("select Изображение.Ссылка " +
                        "from Изображение, Изображения_коллекции " +
                        "where Изображения_коллекции.Номер_коллекции = %d " +
                        "and Изображение.Номер = Изображения_коллекции.Номер_изображения " +
                        "limit 3", collectionId));
                ArrayList<Image> previewImages = new ArrayList<>();
                while (result2.next()) {
                    Image image = new Image();
                    image.setLink(result2.getString("Ссылка"));
                    previewImages.add(image);
                }
                collections.add(new Collection(
                        collectionId,
                        result.getString("Название"),
                        result.getString("Описание"),
                        result.getInt("Номер_пользователя"),
                        collectionType,
                        result.getDate("Дата_создания"),
                        result.getDate("Дата_последнего_изменения"),
                        previewImages));
            }
            result2.close();
            statement2.close();
        }
        result.close();
        statement.close();
        connection.close();
        return collections;
    }

    public ArrayList<Image> selectCollectionImages(final String collectionId) throws SQLException, ClassNotFoundException {
        ArrayList<Image> images = new ArrayList<>();
        return images;
    }

    public void updateUser(final User user) throws SQLException, ClassNotFoundException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        statement.executeUpdate(String.format(
                "update Пользователь set Имя_пользователя='%s', Фамилия='%s', Имя='%s', Отчество='%s', Почта='%s', Описание='%s', Ссылка_фото='%s' where Номер = '%d'",
                user.getUserName(), user.getSecondName(), user.getName(), user.getPatronymic(), user.getEmail(), user.getDescription(), user.getPhotoLink(), user.getId()));
        statement.close();
        connection.close();
    }

    public boolean updateUserPassword(final String prevPassword, final String newPassword, final String userName) throws SQLException, ClassNotFoundException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(String.format("select Пароль from Пользователь where Имя_пользователя = '%s'", userName));
        resultSet.next();
        boolean result = prevPassword.equals(resultSet.getString("Пароль"));
        resultSet.close();
        if (result)
            statement.executeUpdate(String.format("update Пользователь set Пароль='%s' where Имя_пользователя = '%s'", newPassword, userName));
        statement.close();
        connection.close();
        return result;
    }

    public boolean deleteUser(final String password, final String userName) throws SQLException, ClassNotFoundException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(String.format("select Пароль from Пользователь where Имя_пользователя = '%s'", userName));
        resultSet.next();
        boolean result = password.equals(resultSet.getString("Пароль"));
        if (result) {
            resultSet = statement.executeQuery(String.format("select Номер from Пользователь where Имя_пользователя = '%s'", userName));
            resultSet.next();
            int userId = resultSet.getInt("Номер");
            ResultSet resultSet2 = statement.executeQuery(String.format("select Номер from Коллекция where Номер_пользователя = '%d'", userId));
            Statement statement2 = connection.createStatement();
            while (resultSet2.next())
                statement2.executeUpdate(String.format("delete from Изображения_коллекции where Номер_коллекции = '%d'", resultSet2.getInt("Номер")));
            statement2.close();
            resultSet2.close();
            ResultSet resultSet3 = statement.executeQuery(String.format("select Номер from Изображение where Номер_пользователя = '%d'", userId));
            Statement statement3 = connection.createStatement();
            while (resultSet3.next())
                statement3.executeUpdate(String.format("delete from Список_тегов where Номер_изображения = '%d'", resultSet3.getInt("Номер")));
            statement3.close();
            resultSet3.close();
            statement.executeUpdate(String.format("delete from Коллекция where Номер_пользователя = '%d'", userId));
            statement.executeUpdate(String.format("delete from Список_тегов where Номер_изображения = '%d'", userId));
            statement.executeUpdate(String.format("delete from Изображение where Номер_пользователя = '%d'", userId));
            statement.executeUpdate(String.format("delete from Пользователь where Имя_пользователя = '%s'", userName));
        }
        resultSet.close();
        statement.close();
        connection.close();
        return result;
    }

    public int selectNextImageIdByUserId(final int userId) throws SQLException, ClassNotFoundException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(String.format("select count(*) from Изображение where Номер_пользователя = '%d'", userId));
        resultSet.next();
        int lastImageId = resultSet.getInt(1);
        resultSet.close();
        statement.close();
        connection.close();
        return lastImageId;
    }

    public void deleteImageByImageId(final String imageId) throws SQLException, ClassNotFoundException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        statement.executeUpdate(String.format("delete from Список_тегов where Номер_изображения = '%s'", imageId));
        statement.executeUpdate(String.format("delete from Изображения_коллекции where Номер_изображения = '%s'", imageId));
        statement.executeUpdate(String.format("delete from Изображение where Номер = '%s'", imageId));
        statement.close();
        connection.close();
    }

    public void deleteCollectionByCollectionId(final String collectionId) throws SQLException, ClassNotFoundException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        statement.executeUpdate(String.format("delete from Изображения_коллекции where Номер_коллекции = '%s'", collectionId));
        statement.executeUpdate(String.format("delete from Коллекция where Номер = '%s'", collectionId));
        statement.close();
        connection.close();
    }

    public Image selectImageByImageId(final String imageId) throws SQLException, ClassNotFoundException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(String.format("select * from Изображение where Номер = '%s'", imageId));
        resultSet.next();
        Image image = new Image(
                resultSet.getInt("Номер"),
                resultSet.getString("Ссылка"),
                resultSet.getString("Описание"),
                resultSet.getDate("Дата_публикации"),
                resultSet.getDate("Дата_последнего_изменения"),
                resultSet.getInt("Номер_пользователя"));
        resultSet.close();
        statement.close();
        connection.close();
        return image;
    }

    public ArrayList<Collection> selectCollectionByImageIdUserName(final String imageId, final String userName) throws SQLException, ClassNotFoundException {
        Connection connection = ImageryApplication.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(String.format("select distinct Коллекция.* " +
                "from Коллекция, Пользователь, Изображения_коллекции, Изображение " +
                "where Пользователь.Номер = Коллекция.Номер_пользователя " +
                "and Пользователь.Имя_пользователя = '%s'", userName));
        ArrayList<Collection> response = new ArrayList<>();
        while (resultSet.next()) {
            Collection collection = new Collection();
            collection.setId(resultSet.getInt("Номер"));
            collection.setName(resultSet.getString("Название"));
            collection.setDescription(resultSet.getString("Описание"));
            collection.setUserId(resultSet.getInt("Номер_пользователя"));
            collection.setType(resultSet.getInt("Номер_типа") == 1 ? "Открытая" : "Закрытая");
            collection.setCreateDate(resultSet.getDate("Дата_создания"));
            collection.setLastEditDate(resultSet.getDate("Дата_последнего_изменения"));
            response.add(collection);
        }
        resultSet.close();
        statement.close();
        connection.close();
        return response;
    }
}
