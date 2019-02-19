package uk.ac.ic.doc.wacc.ast

import java.util.*

class ActiveScope(var currentScope: Scope, var parentScope: ActiveScope?) {

    fun getSize(): Int {
        return currentScope.getSize() + (parentScope?.getSize() ?: 0)
    }

    fun getPosition(name: String): Int {
        return currentScope.getPosition(name).orElse(currentScope.getSize() + parentScope!!.getPosition(name))
    }

    fun findType(s: String): Optional<Type> =
        Optional.ofNullable(currentScope.definitions[s]?.type).or { parentScope?.findType(s) ?: Optional.empty() }

    fun declare(s: String) {
        val def = currentScope.definitions[s]
        when (def) {
            null -> parentScope!!.declare(s)
            else -> def.isDeclared = true
        }
    }


    fun isVarInCurrScope(s: String): Boolean {
        return currentScope.definitions[s] != null
    }

    fun add(def: Definition) {
        currentScope.definitions[def.name] = Scope.Definition(def.type, false)
    }

    fun addAll(defs: List<Definition>): Boolean {
        defs.forEach {
            if (isVarInCurrScope(it.name)) {
                return false
            }
            add(it)
        }
        return true
    }

    fun newSubScope(scope: Scope = Scope()): ActiveScope =
        ActiveScope(scope, this)

}

