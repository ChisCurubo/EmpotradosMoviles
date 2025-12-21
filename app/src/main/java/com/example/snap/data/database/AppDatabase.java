package com.example.snap.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.snap.data.dao.TranslationHistoryDao;
import com.example.snap.data.dao.UserDao;
import com.example.snap.data.dao.VocabularyDao;
import com.example.snap.data.entities.TranslationHistory;
import com.example.snap.data.entities.User;
import com.example.snap.data.entities.VocabularyItem;

@Database(
        entities = {User.class, TranslationHistory.class, VocabularyItem.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract TranslationHistoryDao translationHistoryDao();
    public abstract VocabularyDao vocabularyDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "translation_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}