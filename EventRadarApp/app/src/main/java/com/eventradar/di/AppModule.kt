package com.eventradar.di

import com.eventradar.data.repository.AuthRepository
import com.eventradar.data.repository.EventRepository
import com.eventradar.data.repository.StorageRepository
import com.eventradar.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth
    ): AuthRepository {
        return AuthRepository(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideEventRepository(
        firestore: FirebaseFirestore
    ): EventRepository {
        return EventRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore, auth: FirebaseAuth, storageRepository: StorageRepository): UserRepository {
        return UserRepository(firestore, auth, storageRepository)
    }
}