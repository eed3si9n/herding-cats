package example
package internal

import scala.reflect.macros.blackbox.Context

object ActMMacro {
  def apply(c0: Context, base: ActMBase): ActMMacro { val c: c0.type } = {
    new ActMMacro { self =>
      val c: c0.type                                 = c0
      // This member is required by `DidTransform`:
      val actMBase: ActMBase                         = base
      // These members are required by `ExprBuilder`:
      // val futureSystem: FutureSystem                             = base.futureSystem
      // val futureSystemOps: futureSystem.Ops {val c: self.c.type} = futureSystem.mkOps(c)
    }
  }
}

private[example] trait ActMMacro extends ActMTransform
  with AnfTransform with TransformUtils /* with Lifter
  with ExprBuilder with AsyncTransform with AsyncAnalysis with LiveVariables*/ {

  val c: Context

  lazy val macroPos = c.macroApplication.pos.makeTransparent
  def atMacroPos(t: c.Tree) = c.universe.atPos(macroPos)(t)
}
