package cy.ac.ucy.cs.anyplace.smas.data

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
        // dsLocalAP: DsLocalChat
) {
  /** Talks to the net */
  val remote = chatRemoteDataSource
  // ** TODO SQLite */
  // val local = dsLocalAP TODO
}