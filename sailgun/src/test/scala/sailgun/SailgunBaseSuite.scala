package sailgun

import sailgun.logging.Logger
import sailgun.logging.Slf4jAdapter

import java.io.PrintStream
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.{ExecutionException, TimeUnit}

import com.martiansoftware.nailgun.{SailgunThreadLocalInputStream, NGServer, ThreadLocalPrintStream}

import monix.eval.Task
import monix.execution.misc.NonFatal
import monix.execution.Scheduler

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import java.nio.charset.StandardCharsets
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import com.martiansoftware.nailgun.examples.Heartbeat
import java.net.InetAddress
import com.martiansoftware.nailgun.NGListeningAddress
import com.martiansoftware.nailgun.NGConstants
import com.martiansoftware.nailgun.Alias

/**
 * Base class for writing test for the nailgun integration.
 */
class NailgunTestUtils extends BaseSuite {

  protected final val TEST_PORT = 8996
  private final val nailgunPool = Scheduler.computation(parallelism = 2)

  /**
   * Starts a Nailgun server, creates a client and executes operations with that client.
   * The server is shut down at the end of `op`.
   *
   * @param log  The logger that will receive all produced output.
   * @param config The config directory in which the client will be.
   * @param noExit Don't exit, the client is responsibleor exiting the server.
   * @param op   A function that will receive the instantiated Client.
   * @return The result of executing `op` on the client.
   */
  def withServerTask[T](logger: Logger, cwd: Path, noExit: Boolean)(
      op: (InputStream, OutputStream, OutputStream) => T
  ): Task[T] = {
    val currentIn = System.in
    val currentOut = System.out
    val currentErr = System.err

    val serverIn = new PipedInputStream()
    val clientOut = new PipedOutputStream(serverIn)

    val clientIn = new PipedInputStream()
    val serverOut = new PrintStream(new PipedOutputStream(clientIn))
    val serverErr = new PrintStream(new ByteArrayOutputStream())

    val localIn = new SailgunThreadLocalInputStream(serverIn)
    val localOut = new ThreadLocalPrintStream(serverOut)
    val localErr = new ThreadLocalPrintStream(serverOut)
    localIn.init(serverIn)
    localOut.init(serverOut)
    localErr.init(serverErr)
    System.in.synchronized {
      System.setIn(localIn)
      System.setOut(localOut)
      System.setErr(localErr)
    }

    val serverIsStarted = scala.concurrent.Promise[Unit]()
    val serverIsFinished = scala.concurrent.Promise[Unit]()
    val serverLogic = Task {
      // Trick nailgun into thinking these are the real streams
      import java.net.InetAddress
      val addr = InetAddress.getLoopbackAddress
      import monix.execution.misc.NonFatal
      try {
        val server = launchServer(localIn, localOut, localErr, addr, TEST_PORT, logger)
        serverIsStarted.success(())
        server.run()
        serverIsFinished.success(())
      } catch {
        case NonFatal(t) =>
          currentErr.println("Error when starting server")
          t.printStackTrace(currentErr)
          serverIsStarted.failure(t)
          serverIsFinished.failure(t)
      } finally {
        serverOut.flush()
        serverErr.flush()
      }
    }

    val client = new Client(clientIn, clientOut, clientOut, TEST_PORT, logger, config)
    def clientCancel(t: Option[Throwable]) = Task {
      serverOut.flush()
      serverErr.flush()

      t.foreach(t => log.trace(t))
      if (!noExit) {
        /* Exit on Windows seems to return a failing exit code (but no logs are logged).
         * This suggests that the nailgun 'exit' method isn't Windows friendly somehow, but
         * for the sake of development I'm merging this since this method will be rarely called. */
        if (CrossPlatform.isWindows) {
          val exitStatusCode = client.issue("exit")
          log.debug(s"The status code for exit in Windows was ${exitStatusCode}.")
        } else client.expectSuccess("exit")
      }

      System.in.synchronized {
        System.setIn(currentIn)
        System.setOut(currentOut)
        System.setErr(currentErr)
      }
    }

    val clientLogic =
      Task(op(log, client)).doOnFinish(clientCancel(_)).doOnCancel(clientCancel(None))
    val startTrigger = Task.fromFuture(serverIsStarted.future)
    val endTrigger = Task.fromFuture(serverIsFinished.future)
    val runClient = {
      for {
        _ <- startTrigger
        value <- clientLogic
        _ <- endTrigger
      } yield value
    }

    // These tests can be flaky on Windows, so if they fail we restart them up to 3 times
    Task
      .zip2(serverLogic, runClient)
      .map(t => t._2)
      .timeout(FiniteDuration(20, TimeUnit.SECONDS))
  }

  def launchServer(
      in: InputStream,
      out: PrintStream,
      err: PrintStream,
      addr: InetAddress,
      port: Int,
      logger: Logger
  ): NGServer = {
    val javaLogger = new Slf4jAdapter(logger)
    val address = new NGListeningAddress(addr, port)
    val poolSize = NGServer.DEFAULT_SESSIONPOOLSIZE
    val heartbeatMs = NGConstants.HEARTBEAT_TIMEOUT_MILLIS.toInt
    val server = new NGServer(address, poolSize, heartbeatMs, in, out, err, javaLogger)
    server.setAllowNailsByClassName(false)
    val aliases = server.getAliasManager
    aliases.addAlias(new Alias("heartbeat", "Run `Heartbeat` naigun server example.", classOf[Heartbeat]))
    server
  }

  /**
   * Starts a Nailgun server, creates a client and executes operations with that client.
   * The server is shut down at the end of `op`.
   *
   * @param log  The logger that will receive all produced output.
   * @param config The config directory in which the client will be.
   * @param noExit Don't exit, the client is responsible for exiting the server.
   * @param op   A function that will receive the instantiated Client.
   * @return The result of executing `op` on the client.
   */
  def withServer[T](config: Path, noExit: Boolean, log: => RecordingLogger)(
      op: (RecordingLogger, Client) => T
  ): T = {
    // These tests can be flaky on Windows, so if they fail we restart them up to 3 times
    val f = withServerTask(log, config, noExit)(op).runAsync(nailgunPool)
    // Note we cannot use restart because our task uses promises that cannot be completed twice
    //val f = f0.onErrorFallbackTo(f0.onErrorFallbackTo(f0)).runAsync(nailgunPool)
    try Await.result(f, FiniteDuration(20, TimeUnit.SECONDS))
    catch {
      case e: ExecutionException => throw e.getCause()
      case t: Throwable => throw t
    } finally f.cancel()
  }

}
