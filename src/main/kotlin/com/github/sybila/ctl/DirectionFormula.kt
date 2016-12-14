package com.github.sybila.ctl


sealed class DirectionFormula {

    interface Unary<T> where T : DirectionFormula, T : Unary<T> {
        val inner: DirectionFormula
        fun copy(inner: DirectionFormula = this.inner): T
    }

    interface Binary<T> where T : DirectionFormula, T : Binary<T> {
        val left: DirectionFormula
        val right: DirectionFormula
        fun copy(left: DirectionFormula = this.left, right: DirectionFormula = this.right): T
    }

    sealed class Atom : DirectionFormula() {

        object True : Atom() {
            override fun toString(): String = "true"
        }
        object False : Atom() {
            override fun toString(): String = "false"
        }

        class Proposition(val name: String, val facet: Facet) : Atom() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other?.javaClass != javaClass) return false

                other as Proposition

                if (name != other.name) return false
                if (facet != other.facet) return false

                return true
            }

            override fun hashCode(): Int {
                var result = name.hashCode()
                result = 31 * result + facet.hashCode()
                return result
            }

            override fun toString(): String = "$name$facet"
        }

        internal class Reference(val name: String) : Atom() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other?.javaClass != javaClass) return false

                other as Reference

                if (name != other.name) return false

                return true
            }

            override fun hashCode(): Int {
                return name.hashCode()
            }

            override fun toString(): String = name
        }

    }


    class Not(override val inner: DirectionFormula) : DirectionFormula(), Unary<Not> {

        override fun copy(inner: DirectionFormula): Not = Not(inner)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Not

            if (inner != other.inner) return false

            return true
        }

        override fun hashCode(): Int {
            return inner.hashCode()
        }

        override fun toString(): String = "!$inner"
    }

    sealed class Bool<T: Bool<T>>(
            override val left: DirectionFormula, override val right: DirectionFormula,
            private val op: String
    ) : DirectionFormula(), Binary<T> {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Bool<*>

            if (left != other.left) return false
            if (right != other.right) return false

            return true
        }

        override fun hashCode(): Int {
            var result = left.hashCode()
            result = 31 * result + right.hashCode()
            return result
        }

        override fun toString(): String = "($left $op $right)"

        class And(left: DirectionFormula, right: DirectionFormula) : Bool<And>(left, right, "&&") {
            override fun copy(left: DirectionFormula, right: DirectionFormula): And = And(left, right)
        }
        class Or(left: DirectionFormula, right: DirectionFormula) : Bool<Or>(left, right, "||") {
            override fun copy(left: DirectionFormula, right: DirectionFormula): Or = Or(left, right)
        }
        class Implies(left: DirectionFormula, right: DirectionFormula) : Bool<Implies>(left, right, "->") {
            override fun copy(left: DirectionFormula, right: DirectionFormula): Implies = Implies(left, right)
        }
        class Equals(left: DirectionFormula, right: DirectionFormula) : Bool<Equals>(left, right, "<->") {
            override fun copy(left: DirectionFormula, right: DirectionFormula): Equals = Equals(left, right)
        }
    }

}