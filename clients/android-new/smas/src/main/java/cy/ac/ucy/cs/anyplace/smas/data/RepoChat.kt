package cy.ac.ucy.cs.anyplace.smas.data

import cy.ac.ucy.cs.anyplace.smas.data.source.SmasLocalDS
import cy.ac.ucy.cs.anyplace.smas.data.source.ChatRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smas Chat Repository:
 * Has
 * - [ChatRemoteDataSource]: Chat Remote Server connection
 * - [DsLocalAP]: Chat Local Storage TODO Room/SQLite
 */
@Singleton
class RepoChat @Inject constructor(
        chatRemoteDataSource: ChatRemoteDataSource,
        dsLocalAP: SmasLocalDS) {
  /** Talks to the net */
  val remote = chatRemoteDataSource
  /** Talks to SQLite */
  val local = dsLocalAP
}