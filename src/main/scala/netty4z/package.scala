import io.netty.util.concurrent.Future
import zio.internal.tracing.TracingConfig
import zio.{Task, UIO, ZIO, ZQueue}

package object netty4z {

  val runtime: zio.Runtime[zio.ZEnv] = zio.Runtime.default.withFatal{
    e =>
      println(s"THIS IS FATAL: $e")
      false
  }

  implicit class NettyFutureToZIO[A](fTask: Task[Future[A]]) {
    def toZIO: Task[A] = {
      fTask.flatMap { f =>
        ZIO.effectAsyncInterrupt { callback =>
          if (f.isDone) Right(complete(f))
          else {
            f.addListener { (res: Future[A]) => callback(complete(res)) }
            Left(UIO {
              Logger.debug("Cancelling")
              if (f.isCancellable) f.cancel(true)
            })
          }
        }
      }
    }
  }

  private def complete[A](res: Future[A]): Task[A] = {
    if (res.isSuccess) ZIO.succeed(res.getNow)
    else ZIO.fail {
      val cause = res.cause()
      Logger.logConnectionReset(cause)
      cause
    }
  }

  type TaskQueue[A] = ZQueue[Any, Any, Throwable, Throwable, A, A]

}
