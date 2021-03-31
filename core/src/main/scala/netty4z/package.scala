import io.netty.util.concurrent.Future
import netty4z.utils.{BufferUtils, Logger}
import zio.{Queue, Task, UIO, ZIO}

package object netty4z extends BufferUtils.Ops {

  val zRuntime: zio.Runtime[zio.ZEnv] = zio.Runtime.default

  implicit class NettyFutureToZIO[A](fTask: Task[Future[A]]) {
    def toZIO: Task[A] = {
      fTask.flatMap { f =>
        ZIO.effectAsyncInterrupt { callback =>
          if (f.isDone) Right(complete(f))
          else {
            f.addListener { (res: Future[A]) => callback(complete(res)) }
            Left(UIO {
              if (f.isCancellable) f.cancel(true)
            })
          }
        }
      }
    }

    private def complete(res: Future[A]): Task[A] = {
      if (res.isSuccess) ZIO.succeed(res.getNow)
      else ZIO.fail {
        val cause = res.cause()
        Logger.logConnectionReset(cause)
        cause
      }
    }
  }

  implicit class ZQueueOps[A](val q: Queue[A]) extends AnyVal {
    def offerIfNotShutdown(a: A): UIO[Boolean] = {
      q.offer(a).catchAllCause { c =>
        q.isShutdown.flatMap { down =>
          if (down && c.interrupted) UIO(false)
          else ZIO.halt(c)
        }
      }
    }

    def takeIfNotShutdown(default: => A): UIO[A] = {
      q.take.catchAllCause { c =>
        q.isShutdown.flatMap { down =>
          if (down && c.interrupted) UIO(default)
          else ZIO.halt(c)
        }
      }
    }
  }

}
