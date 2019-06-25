package sailgun.logging

import java.io.PrintStream

final class SailgunLogger(out: PrintStream) extends Logger {
    def debug(msg: String): Unit = out.println(s"[debug] $msg")
    def error(msg: String): Unit = out.println(s"[error] $msg")
    def warn(msg: String): Unit = out.println(s"[warn] $msg")
    def info(msg: String): Unit = out.println(s"[info] $msg")
    def trace(exception: Throwable): Unit = exception.printStackTrace(out)
}