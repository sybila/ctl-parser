package com.github.sybila.huctl

/**
 * An HUCTLp Formula that can be used to describe general temporal properties.
 *
 * It supports [Transition] and [Numeric] propositions together with standard true/false literals and boolean logic.
 *
 * Furthermore, it supports the basic temporal operators: [Next] (X), [Future] (F), [Globally] (G) and [Until] (U),
 * plus the special weak operators ([WeakFuture] and [WeakNext]) together with path quantification
 * (see [PathQuantifier]) and optional direction modifiers (see [DirFormula]).
 *
 * Finally, it also supports first order ([ForAll] and [Exists]) and hybrid ([Bind] and [At]) operators.
 *
 * Semantically, an HUCTLp formula is evaluated at a state of a transition system. For full description
 * of the semantics, see the project readme.
 *
 * Note that each formula is either [Binary], [Unary] or an atomic proposition.
 */
sealed class Formula(
        private val string: String
) {

    // Transform this formula into a direction formula if possible
    internal open fun asDirFormula(): DirFormula? = null

    /* ========== Atoms ========== */

    /**
     * Logical tautology. Any state satisfies this formula.
     */
    object True : Formula("true") {
        override fun asDirFormula(): DirFormula? = DirFormula.True
    }

    /**
     * Logical contradiction. No state satisfies this formula.
     */
    object False : Formula("false") {
        override fun asDirFormula(): DirFormula? = DirFormula.False
    }

    /**
     * The semantics of the reference formula are not defined. The parser will never
     * generate a formula which contains an unresolved reference. However, it can be
     * useful to use objects of this type to reference some precomputed results or
     * properties not supported by the HUCTLp syntax directly.
     */
    data class Reference(
            /** A unique name of the data referenced by this object */
            val name: String
    ) : Formula(name) {
        override fun asDirFormula(): DirFormula? = DirFormula.Reference(name)
    }

    /**
     * Transition proposition. A state satisfies this proposition if there is a transition
     * in the specified direction (up/down) and flow in/out of the state.
     */
    data class Transition(
            /** The name of the variable which should perform the transition */
            val name: String,
            /** The direction (up/down) of the variable change (relative to this state) */
            val direction: Direction,
            /** The flow in the specified direction (in/out) */
            val flow: Flow)
        : Formula("$name:$flow$direction")

    /**
     * Numeric proposition. The semantics of this proposition are not strictly defined.
     * However, usually each state maps to a certain point/area in a cartesian space. In
     * this case, the proposition outlines an area of valid states.
     */
    data class Numeric(
            /** Left side of the comparison */
            val left: Expression,
            /** Comparison operator */
            val cmp: CompareOp,
            /** Right side of the comparison */
            val right: Expression)
        : Formula("($left $cmp $right)")

    /* ========== Hybrid ========== */

    /**
     * First-order forall operator. Specifies that for all states in [bound] substituted for [name], the
     * [target] formula is valid at inspected state.
     *
     * Note that `(forall x in A : B)` is semantically equivalent to `(forall x : ((at x: A) -> B))`.
     * However, we allow the `in` syntax because it is much more readable and can yield interesting
     * performance optimisations.
     *
     * Warning: [name] must usually be a free name (not assigned yet).
     */
    data class ForAll(
            /** The name that should be substituted in the [target] formula. */
            val name: String,
            /** The bound formula which limits the states considered for substitution. */
            val bound: Formula,
            /** The target formula which is subject to substitution. */
            val target: Formula)
        : Formula("(forall $name in $bound : $target)"), Binary<ForAll, Formula> {
        override val left: Formula = bound; override val right: Formula = target
        override fun copy(left: Formula, right: Formula): ForAll = this.copy(bound = left, target = right)
    }

    /**
     * First-order exists operator. Specifies that there exists a state in [bound], such that when substituted
     * for [name], the [target] formula is valid at the inspected state.
     *
     * Note that `(exists x in A : B)` is semantically equivalent to `(exists x : ((at x: A) && B))`.
     * However, we allow the `in` syntax because it is much more readable and can yield interesting
     * performance optimisations.
     *
     * Warning: [name] must usually be a free name (not assigned yet).
     */
    data class Exists(
            /** The name that should be substituted in the [target] formula. */
            val name: String,
            /** The bound formula which limits the states considered for substitution */
            val bound: Formula,
            /** The target formula which is subject to substitution */
            val target: Formula)
        : Formula("(exists $name in $bound : $target)"), Binary<Exists, Formula> {
        override val left: Formula = bound; override val right: Formula = target
        override fun copy(left: Formula, right: Formula): Exists = this.copy(bound = left, target = right)
    }

    /**
     * Hybrid operator which specifies that at the inspected state, the [target] formula holds
     * with [name] substituted for the inspected state.
     *
     * Warning: [name] must usually be a free name (not assigned yet).
     */
    data class Bind(
            /** The name that should be substituted in the [target] formula */
            val name: String,
            /** The target formula which is subject to substitution */
            val target: Formula)
        : Formula("(bind $name : $target)"), Unary<Bind, Formula> {
        override val inner: Formula = target
        override fun copy(inner: Formula): Bind = this.copy(target = inner)
    }

    /**
     * At operator specifies that a formula holds at the state with the given [name].
     *
     * Warning: [name] must be a bound name (assigned).
     */
    data class At(
            /** The name of the state that should be considered as a new point of interest */
            val name: String,
            /** The formula which should be inspected at the new point of interest */
            val target: Formula)
        : Formula("(at $name  $target)"), Unary<At, Formula> {
        override val inner: Formula = target
        override fun copy(inner: Formula): At = this.copy(target = inner)
    }

    /* ========== Boolean ========== */

    /**
     * Logical negation. A state satisfies this formula if it does not satisfy [inner].
     */
    data class Not(override val inner: Formula) : Formula("!$inner"), Unary<Not, Formula>

    /**
     * Logical conjunction. A state satisfies this formula if it satisfies both [left] and [right].
     */
    data class And(override val left: Formula, override val right: Formula)
        : Formula("($left && $right)"), Binary<And, Formula> {
        override fun asDirFormula(): DirFormula? = this.directionFold(DirFormula::And)
    }

    /**
     * Logical disjunction. A state satisfies this formula if it satisfies any of the [left] and [right] formulas.
     */
    data class Or(override val left: Formula, override val right: Formula)
        : Formula("($left && $right)"), Binary<Or, Formula> {
        override fun asDirFormula(): DirFormula? = this.directionFold(DirFormula::Or)
    }

    /**
     * Logical implication. A state satisfies this formula if it does not satisfy [left] or if it
     * satisfies both [left] and [right].
     */
    data class Implies(override val left: Formula, override val right: Formula)
        : Formula("($left -> $right)"), Binary<Implies, Formula> {
        override fun asDirFormula(): DirFormula? = this.directionFold(DirFormula::Implies)
    }

    /**
     * Logical equivalence. A state satisfies this formula if it does not satisfy neither [left] nor [right] or
     * if it satisfies both.
     */
    data class Equals(override val left: Formula, override val right: Formula)
        : Formula("($left <-> $right)"), Binary<Equals, Formula> {
        override fun asDirFormula(): DirFormula? = this.directionFold(DirFormula::Equals)
    }

    /* ========== Temporal, Simple ========== */

    /**
     * Temporal next (X) operator. A state satisfies next, if direct successors specified by the [quantifier]
     * match the [direction] and satisfy the [inner] formula.
     *
     * Note that for the [PathQuantifier.A] and [PathQuantifier.pA] this means that all paths
     * must satisfy the [direction]. See [WeakNext] for less strict alternative.
     */
    data class Next(
            override val quantifier: PathQuantifier, override val inner: Formula, override val direction: DirFormula
    ) : Formula("({$direction}${quantifier}X $inner)"), Unary<Next, Formula>, Temporal {
        override fun copy(inner: Formula): Next = this.copy(inner = inner)
    }

    /**
     * Temporal future (F) operator. A state satisfies future if the paths specified by the [quantifier]
     * match the [direction] and eventually satisfy the [inner] formula.
     *
     * Note that for the [PathQuantifier.A] and [PathQuantifier.pA] this means that all paths
     * must satisfy the [direction]. See [WeakFuture] for less strict alternative.
     */
    data class Future(
            override val quantifier: PathQuantifier, override val inner: Formula, override val direction: DirFormula
    ) : Formula("({$direction}${quantifier}F $inner)"), Unary<Future, Formula>, Temporal {
        override fun copy(inner: Formula): Future = this.copy(inner = inner)
    }

    /**
     * Temporal globally (G) operator. A state satisfies globally if the paths specified by the [quantifier]
     * match the [direction] and always satisfy the [inner] formula.
     */
    data class Globally(
            override val quantifier: PathQuantifier, override val inner: Formula, override val direction: DirFormula
    ) : Formula("({$direction}${quantifier}G $inner)"), Unary<Globally, Formula>, Temporal {
        override fun copy(inner: Formula): Globally = this.copy(inner = inner)
    }

    /**
     * Weak temporal next (wX) operator. A state satisfies weak next when all successors
     * (specified by the [quantifier]) that match the [direction] also satisfy the [inner] formula.
     *
     * Note that this is essentially equivalent to [Next], but instead of conjunction, the direction requirement is
     * an implication.
     *
     * When [direction] is [DirFormula.True], [WeakNext] and [Next] are equivalent.
     */
    data class WeakNext(
            override val quantifier: PathQuantifier, override val inner: Formula, override val direction: DirFormula
    ) : Formula("({$direction}${quantifier}wX $inner)"), Unary<WeakNext, Formula>, Temporal {
        override fun copy(inner: Formula): WeakNext = this.copy(inner = inner)
    }

    /**
     * Weak temporal future (wF) operator. A state satisfying weak future when all paths
     * (specified by the [quantifier]) that match the [direction] also satisfy the [inner] formula.
     *
     * Note that this is essentially equivalent to [Future], but instead of conjunction, the direction requirement is
     * an implication.
     *
     * When [direction] is [DirFormula.True], [WeakFuture] and [Future] are equivalent.
     */
    data class WeakFuture(
            override val quantifier: PathQuantifier, override val inner: Formula, override val direction: DirFormula
    ) : Formula("({$direction}${quantifier}wF $inner)"), Unary<WeakFuture, Formula>, Temporal {
        override fun copy(inner: Formula): WeakFuture = this.copy(inner = inner)
    }

    /* ========== Temporal, Until ========== */

    /**
     * Temporal until (U) operator. A state satisfies until if the paths specified by the [quantifier]
     * match the [direction] and always satisfy the [path] formula until the eventually, the [reach]
     * formula is satisfied.
     */
    data class Until(
            override val quantifier: PathQuantifier,
            /** The formula which needs to be valid along a path until reach is found. */
            val path: Formula,
            /** The formula which needs to be eventually found in the path. */
            val reach: Formula,
            override val direction: DirFormula
    ) : Formula("($path {$direction}${quantifier}U $reach)"), Binary<Until, Formula>, Temporal {
        override val left: Formula = path ; override val right: Formula = reach
        override fun copy(left: Formula, right: Formula): Until = this.copy(path = left, reach = right)
    }

    /**
     * Return string which uniquely represents this formula and can be parsed to create an equivalent object.
     */
    override fun toString(): String = string

}