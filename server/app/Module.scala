import com.google.inject.AbstractModule

/**
 * Binding the eager-singleton that will bootstrap the application.
 * Dependency Injection (DI) essentially starts from this point.
 */
class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[Anyplace]).asEagerSingleton()
  }
}
