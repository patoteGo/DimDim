package com.EYP.dimdim.di

import android.content.Context
import androidx.room.Room
import com.EYP.dimdim.data.database.DimDimDatabase
import com.EYP.dimdim.data.database.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DimDimDatabase {
        return Room.databaseBuilder(
            context,
            DimDimDatabase::class.java,
            DimDimDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideTransactionDao(database: DimDimDatabase): TransactionDao {
        return database.transactionDao()
    }
}