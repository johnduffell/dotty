package dotty.tools
package dotc

import core.Contexts.Context
import reporting.Reporter
import java.io.EOFException
import scala.annotation.tailrec

/** A compiler which stays resident between runs.
 *  Usage:
 *
 *  > scala dotty.tools.dotc.Resident <options> <initial files>
 *
 *  dotc> "more options and files to compile"
 *
 *  ...
 *
 *  dotc> :reset  // reset all options to the ones passed on the command line
 *
 *  ...
 *
 *  dotc> :q     // quit
 */
object Resident extends Driver {

  @unshared object residentCompiler extends Compiler

  override def newCompiler(): Compiler = ???

  override def sourcesRequired = false

  private val quit = ":q"
  private val reset = ":reset"

  private def getLine() = {
    Console.print(prompt)
    try scala.io.StdIn.readLine() catch { case _: EOFException => quit }
  }

  final override def process(args: Array[String], rootCtx: Context): Reporter = {
    @tailrec def loop(args: Array[String], prevCtx: Context): Reporter = {
      var (fileNames, ctx) = setup(args, prevCtx)
      doCompile(residentCompiler, fileNames)(ctx)
      var nextCtx = ctx
      var line = getLine()
      while (line == reset) {
        nextCtx = rootCtx
        line = getLine()
      }
      if (line.startsWith(quit)) ctx.typerState.reporter
      else loop(line split "\\s+", nextCtx)
    }
    loop(args, rootCtx)
  }
}
