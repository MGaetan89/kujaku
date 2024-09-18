package io.ona.kujaku.sample.repository;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

public class BaseRepository {

    private KujakuRepository repository;

    public BaseRepository(KujakuRepository repository) {
        this.repository = repository;
    }

    public KujakuRepository getRepository() {
        return repository;
    }

    public SQLiteDatabase getWritableDatabase() {
        return this.repository.getWritableDatabase();
    }

    public SQLiteDatabase getReadableDatabase() {
        return this.repository.getReadableDatabase();
    }
}
