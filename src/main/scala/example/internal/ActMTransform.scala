package example
package internal

trait ActMTransform { self: ActMMacro =>
  import c.universe._
  import c.internal._

  type Binding = (TermName, Tree)
  type BindGroup = (List[Binding], Tree)

  val TMPVAR_PREFIX = "$tmc$"

  lazy val monadInstance: Tree = {
    val tree = c.macroApplication
    val appliedMonad = typeRef(NoPrefix, typeOf[cats.Monad[Nothing]].typeSymbol, List(tree.tpe.typeConstructor))
    c.inferImplicitValue(appliedMonad)
  }

  def actMTransform(body: Tree): Tree = {
    val tree1 = anfTransform(body)
    val tree2 = transform(tree1)
    val tree3 = c.typecheck(tree2)
    // println(tree3)
    tree3
  }

  def transform(tree: Tree): Tree =
    transform(extractBindings(tree), true)

  def transform(group: BindGroup, isPure: Boolean): Tree =
    group match {
      case (binds, tree) =>
        binds match {
          case Nil =>
            if (isPure) q"""$monadInstance.pure($tree)"""
            else tree
          case (name, unwrappedFrom) :: xs =>
            val innerTree = transform((xs, tree), isPure)
            val param = ValDef(Modifiers(Flag.PARAM), name, TypeTree(), EmptyTree)
            q"""$monadInstance.flatMap($unwrappedFrom) { $param => $innerTree }"""
        }
    }

  def extractBindings(tree: Tree): BindGroup =
    {
      extractUnwrap(tree) getOrElse { tree match {
        case ValDef(mod, lhs, typ, rhs) =>
          val (binds, newRhs) = extractBindings(rhs)
          (binds, ValDef(mod, lhs, typ, newRhs))
        case Block(stats, expr) => 
          val (binds, newBlock) = extractBlock(stats :+ expr)
          extractUnwrap(binds, newBlock)
        case _ =>
          (Nil, tree)
      }}
    }

  def extractBlock(stmts: List[Tree]): BindGroup =
    (stmts : @unchecked) match {
      case expr :: Nil  => (Nil, Block(Nil, transform(expr)))
      case stmt :: rest =>
        val (bindings, newTree) = extractBindings(stmt)
        // The newTree might not actually be a statement but just a standalone identifier,
        // and as of 2.10.2 "pure" expressions in statement position are considered an error,
        // so we excise them here.
        val newStmt = List(newTree) filter ({
          case Ident(_) => false
          case _ => true
        })
        val restGrp@(restBindings, Block(restStmts, expr)) = extractBlock(rest)
        val newBlock = 
          if (restBindings.isEmpty) Block(newStmt ++ restStmts, expr)
          else Block(newStmt, transform(restGrp, isPure = false))
        (bindings, newBlock)
    }

  def extractUnwrap(tree: Tree): Option[BindGroup] =
    getUnwrapArgs(tree) map { case (arg, _) =>
      val (binds, newArg) = extractBindings(arg)
      extractUnwrap(binds, newArg)
    }
  def extractUnwrap(binds: List[Binding], tree: Tree): BindGroup =
    {
      val freshName = getFreshName()
      (binds :+ ((freshName, tree)), Ident(freshName))
    }
  def getFreshName(): TermName = TermName(c.freshName(TMPVAR_PREFIX))

  def getUnwrapArgs(tree: Tree): Option[(Tree, Tree)] =
    tree match {
      case Select(Apply(Apply(fun, List(arg)), List(unapply)), op)
        if // isEffectfulToUnwrappable(fun) && 
        (op == TermName("next")) => Some((arg, unapply))
      case _ => None
    }
}
