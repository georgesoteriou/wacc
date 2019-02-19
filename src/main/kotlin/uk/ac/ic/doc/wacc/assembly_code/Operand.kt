package uk.ac.ic.doc.wacc.assembly_code

sealed class Operand {

    data class Register(var number:Int): Operand(){
        override fun toString(): String = "r$number"
    }
    sealed class Literal: Operand() {
        data class LInt(var value:String): Literal() {
            override fun toString(): String = "=$value"
        }
        data class LChar(var value:Char): Literal() {
            override fun toString(): String = "#'$value'"
        }
        data class LBool(var value:Boolean): Literal() {
            override fun toString(): String = "#${if(value) {1} else {0}}"
        }
        data class LString(var value:String): Literal() {
            //TODO: Strings
            override fun toString(): String = "TODO"
        }
    }
    data class Offset(var value: Int): Operand() {
        override fun toString(): String = "#$value"
    }
    object Lr: Operand() {
        override fun toString(): String = "lr"
    }
    object Pc: Operand() {
        override fun toString(): String = "pc"
    }
    object Sp: Operand() {
        override fun toString(): String = "sp"
    }
}