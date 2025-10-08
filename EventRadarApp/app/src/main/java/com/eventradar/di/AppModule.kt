package com.eventradar.di

import com.eventradar.data.repository.AuthRepository
import com.eventradar.data.repository.CloudinaryRepository
import com.eventradar.data.repository.CommentRepository
import com.eventradar.data.repository.EventRepository
import com.eventradar.data.repository.FilterRepository
import com.eventradar.data.repository.LocationRepository
import com.eventradar.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.imperiumlabs.geofirestore.GeoFirestore
import javax.inject.Named
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
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth
    ): AuthRepository {
        return AuthRepository(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideEventRepository(
        firestore: FirebaseFirestore,
        geoFirestore: GeoFirestore,
        filterRepository: FilterRepository,
        locationRepository: LocationRepository,
        authRepository: AuthRepository,
        userRepository: UserRepository
    ): EventRepository {
        return EventRepository(firestore, geoFirestore, filterRepository, locationRepository, authRepository, userRepository)
    }

    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore, auth: FirebaseAuth, cloudinaryRepository: CloudinaryRepository): UserRepository {
        return UserRepository(firestore, auth, cloudinaryRepository)
    }

    @Provides
    @Singleton
    @Named("eventsCollection")
    fun provideEventsCollection(firestore: FirebaseFirestore): CollectionReference {
        return firestore.collection("events")
    }


    @Provides
    @Singleton
    fun provideGeoFirestore(@Named("eventsCollection") eventsCollection: CollectionReference): GeoFirestore {
        return GeoFirestore(eventsCollection)
    }

    @Provides
    @Singleton
    fun provideCommentRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        userRepository: UserRepository
    ): CommentRepository {
        return CommentRepository(firestore, auth, userRepository)
    }
}