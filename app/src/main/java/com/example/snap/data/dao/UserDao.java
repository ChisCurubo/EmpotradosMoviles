package com.example.snap.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.snap.data.entities.User;

@Dao
public interface UserDao {

    @Insert
    void insert(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserById(String userId);

    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<User> getUserByIdLiveData(String userId);

    @Query("DELETE FROM users WHERE id = :userId")
    void deleteUser(String userId);

    @Query("SELECT COUNT(*) FROM users")
    int getCount();
}