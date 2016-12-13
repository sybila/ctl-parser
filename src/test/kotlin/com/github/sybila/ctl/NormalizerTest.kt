package com.github.sybila.ctl

import org.junit.Test
import kotlin.test.assertEquals

class UntilNormalFormTest {

    private val p1 = Formula.Atom.Reference("p1")
    private val p2 = Formula.Atom.Reference("p2")
    private val p3 = Formula.Atom.Reference("p3")
    private val p4 = Formula.Atom.Reference("p4")

    @Test fun complexTest() {

        val f1 = ("var1".asVariable() neq 14.3.asConstant())
        val f2 = ("var2".asVariable() lt (-15.3).asConstant())
        val d1 = "var1".negativeIn()
        val d2 = "var2".positiveOut()

        val prop = EF(AF((EX(f1) AU False) equal not(d2)) implies AX(EG(f2) EU AG(d1)))
        assertEquals(
                True EU
                        (not(True AU ((EX(f1) AU False and not(d2)) or (not(EX(f1) AU False) and not(not(d2)))))
                                or
                                not(EX(not((not(True AU not(f2))) EU (not(True EU not(d1))))))),
                prop.normalize()
        )
    }

    @Test fun nestingNoPropositions() {
        val prop = EF(p1 implies AX(EG(p2) EU AG(p1)))
        assertEquals(
                True EU (not(p1) or not(EX(not((not(True AU not(p2))) EU (not(True EU not(p1))))))),
                prop.normalize()
        )
    }

    @Test fun nestingPreserve() {
        val prop = (p1 and EX(not(p2))) EU ( not(p3) AU (p4 or p2))
        assertEquals(prop, prop.normalize())
    }

    //trivial cases

    @Test fun equivChange() {
        val prop = p1 equal p2
        val norm = prop.normalize()
        assertEquals((p1 and p2) or (not(p1) and not(p2)), norm)
        assertEquals(norm, norm.normalize())
    }

    @Test fun implChange() {
        val prop = p1 implies p2
        val norm = prop.normalize()
        assertEquals(not(p1) or p2, norm)
        assertEquals(norm, norm.normalize())
    }

    @Test fun agChange() {
        val prop = AG(p1)
        val norm = prop.normalize()
        assertEquals(not(True EU  not(p1)), norm)
        assertEquals(norm, norm.normalize())
    }

    @Test fun egChange() {
        val prop = EG(p1)
        val norm = prop.normalize()
        assertEquals(not(True AU not(p1)), norm)
        assertEquals(norm, norm.normalize())
    }

    @Test fun afChange() {
        val prop = AF(p1)
        val norm = prop.normalize()
        assertEquals(True AU p1, norm)
        assertEquals(norm, norm.normalize())
    }

    @Test fun efChange() {
        val prop = EF(p1)
        val norm = prop.normalize()
        assertEquals(True EU p1, norm)
        assertEquals(norm, norm.normalize())
    }

    @Test fun axChange() {
        val prop = AX(p1)
        val norm = prop.normalize()
        assertEquals(not(EX(not(p1))), norm)
        assertEquals(norm, norm.normalize())
    }

    @Test fun auPreserve() {
        val prop = p1 AU p2
        assertEquals(prop, prop.normalize())
    }

    @Test fun euPreserve() {
        val prop = p1 EU p2
        assertEquals(prop, prop.normalize())
    }

    @Test fun orPreserve() {
        val prop = p1 or p2
        assertEquals(prop, prop.normalize())
    }

    @Test fun andPreserve() {
        val prop = p1 and p2
        assertEquals(prop, prop.normalize())
    }

    @Test fun negPreserve() {
        val prop = not(p1)
        assertEquals(prop, prop.normalize())
    }

    @Test fun exPreserve() {
        val prop = EX(p1)
        assertEquals(prop, prop.normalize())
    }

    @Test fun floatPreserve() {
        val prop = ("val".asVariable() ge 32.2.asConstant())
        assertEquals(prop, prop.normalize())
    }

    @Test fun booleanPreserve() {
        assertEquals(True, True.normalize())
        assertEquals(False, False.normalize())
    }

    @Test fun directionPreserve() {
        val prop = "var".positiveIn()
        assertEquals(prop, prop.normalize())
    }



}