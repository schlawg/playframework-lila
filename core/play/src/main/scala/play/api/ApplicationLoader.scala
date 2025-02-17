/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import play.api.inject.ApplicationLifecycle
import play.api.mvc.ControllerComponents
import play.api.mvc.DefaultControllerComponents
import play.utils.Reflect

/**
 * Loads an application.  This is responsible for instantiating an application given a context.
 *
 * Application loaders are expected to instantiate all parts of an application, wiring everything together. They may
 * be manually implemented, if compile time wiring is preferred, or core/third party implementations may be used, for
 * example that provide a runtime dependency injection framework.
 *
 * During dev mode, an ApplicationLoader will be instantiated once, and called once, each time the application is
 * reloaded. In prod mode, the ApplicationLoader will be instantiated and called once when the application is started.
 *
 * Out of the box Play provides a Guice module that defines a Java and Scala default implementation based on Guice,
 * as well as various helpers like GuiceApplicationBuilder.  This can be used simply by adding the "PlayImport.guice"
 * dependency in build.sbt.
 *
 * A custom application loader can be configured using the `play.application.loader` configuration property.
 * Implementations must define a no-arg constructor.
 */
trait ApplicationLoader {

  /**
   * Load an application given the context.
   */
  def load(context: ApplicationLoader.Context): Application
}

object ApplicationLoader {
  import play.api.inject.DefaultApplicationLifecycle

  // Method to call if we cannot find a configured ApplicationLoader
  private def loaderNotFound(): Nothing = {
    sys.error(
      "No application loader is configured. Please configure an application loader either using the " +
        "play.application.loader configuration property, or by depending on a module that configures one. " +
        "You can add the Guice support module by adding \"libraryDependencies += guice\" to your build.sbt."
    )
  }

  private[play] final class NoApplicationLoader extends ApplicationLoader {
    override def load(context: Context): Nothing = loaderNotFound()
  }

  /**
   * The context for loading an application.
   *
   * @param environment The environment
   * @param initialConfiguration The initial configuration.  This configuration is not necessarily the same
   *                             configuration used by the application, as the ApplicationLoader may, through it's own
   *                             mechanisms, modify it or completely ignore it.
   * @param lifecycle Used to register hooks that run when the application stops.
   * @param devContext If an application is loaded in dev mode then this additional context is available.
   */
  final case class Context(
      environment: Environment,
      initialConfiguration: Configuration,
      lifecycle: ApplicationLifecycle
  )

  object Context {

    /**
     * Create an application loading context.
     *
     * Locates and loads the necessary configuration files for the application.
     *
     * @param environment The application environment.
     * @param initialSettings The initial settings. These settings are merged with the settings from the loaded
     *                        configuration files, and together form the initialConfiguration provided by the context.  It
     *                        is intended for use in dev mode, to allow the build system to pass additional configuration
     *                        into the application.
     * @param lifecycle Used to register hooks that run when the application stops.
     * @param devContext If an application is loaded in dev mode then this additional context can be provided.
     */
    def create(
        environment: Environment,
        initialSettings: Map[String, AnyRef] = Map.empty[String, AnyRef],
        lifecycle: ApplicationLifecycle = new DefaultApplicationLifecycle(),
    ): Context = {
      Context(
        environment = environment,
        lifecycle = lifecycle,
        initialConfiguration = Configuration.load(environment, initialSettings)
      )
    }
  }

  /**
   * Locate and instantiate the ApplicationLoader.
   */
  def apply(context: Context): ApplicationLoader = {
    val LoaderKey = "play.application.loader"
    if (!context.initialConfiguration.has(LoaderKey)) {
      loaderNotFound()
    }

    Reflect.configuredClass[ApplicationLoader, NoApplicationLoader](
      context.environment,
      context.initialConfiguration,
      LoaderKey,
      classOf[NoApplicationLoader].getName
    ) match {
      case None =>
        loaderNotFound()
      case Some(scalaClass) =>
        scalaClass.getDeclaredConstructor().newInstance()
    }
  }
}

abstract class BuiltInComponentsFromContext(context: ApplicationLoader.Context) extends BuiltInComponents {

  override def environment: Environment                   = context.environment
  override def applicationLifecycle: ApplicationLifecycle = context.lifecycle
  override def configuration: Configuration               = context.initialConfiguration

  lazy val controllerComponents: ControllerComponents = DefaultControllerComponents(
    defaultActionBuilder,
    playBodyParsers,
    fileMimeTypes,
    executionContext
  )
}
