package com.example.wellbeingplayground.interfaces

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.wellbeingplayground.database.WalkingDatabase
import com.example.wellbeingplayground.interfaces.Constants.KEY_FIRST_TIME_TOGGLE
import com.example.wellbeingplayground.interfaces.Constants.KEY_NAME
import com.example.wellbeingplayground.interfaces.Constants.KEY_WEIGHT
import com.example.wellbeingplayground.interfaces.Constants.SHARED_PREFERENCES_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWalkingDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        WalkingDatabase::class.java,
        Constants.WALKING_DATABASE_NAME
    ).build()

    @Provides
    @Singleton
    fun provideWalkDao(database:WalkingDatabase) = database.getWalkDao()

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext app:Context) =
        app.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideName(sharedPreferences: SharedPreferences) = sharedPreferences.getString(KEY_NAME,"") ?: ""

    @Singleton
    @Provides
    fun provideWeight(sharedPreferences: SharedPreferences) = sharedPreferences.getFloat(KEY_WEIGHT,80f)

    @Singleton
    @Provides
    fun provideFirstTimeToggle(sharedPreferences: SharedPreferences) = sharedPreferences.getBoolean(KEY_FIRST_TIME_TOGGLE,true)
}