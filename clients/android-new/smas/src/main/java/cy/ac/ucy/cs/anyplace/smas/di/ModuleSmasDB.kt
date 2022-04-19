package cy.ac.ucy.cs.anyplace.smas.di

import android.content.Context
import androidx.room.Room
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.db.SmasDB
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ModuleSmasDB {

  @Singleton
  @Provides
  fun provideDatabase(
          @ApplicationContext ctx: Context) = Room.databaseBuilder(
          ctx,
          SmasDB::class.java,
          CHAT(ctx).DB_SMAS_NAME)
          .build()

  @Singleton
  @Provides
  fun provideDao(db: SmasDB) = db.DAO()
}
