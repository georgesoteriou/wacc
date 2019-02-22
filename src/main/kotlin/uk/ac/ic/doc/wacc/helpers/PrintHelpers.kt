package uk.ac.ic.doc.wacc.helpers

import uk.ac.ic.doc.wacc.CodeGenerator
import uk.ac.ic.doc.wacc.assembly_code.Instruction
import uk.ac.ic.doc.wacc.assembly_code.Operand
import uk.ac.ic.doc.wacc.ast.Expression
import uk.ac.ic.doc.wacc.ast.Type

fun CodeGenerator.messageTagGenerator(content: String, flag: Boolean = false) {
    var length: Int = content.length
    if (flag) {
        length -= 1
    }
    data.add(Instruction.Flag("msg_$messageCounter:"))
    data.add(Instruction.WORD(length))
    data.add(Instruction.ASCII(content))
    messageCounter += 1
}

fun CodeGenerator.add_pPrintString(tagValue: Int) {
    // This should be called at the end of the program after checking the flags
    // The required message for this: %.*s\0 resides at tagValue (= messageCounter - 1)
    instructions.addAll(
        arrayListOf(
            (Instruction.LABEL("p_print_string")),
            (Instruction.LDRSimple(Operand.Register(1), Operand.Register(0))),
            (Instruction.ADD(
                Operand.Register(2),
                Operand.Register(0),
                Operand.Constant(4)
            )),
            (Instruction.LDRSimple(Operand.Register(0), Operand.MessageTag(tagValue))),
            (Instruction.ADD(
                Operand.Register(0),
                Operand.Register(0),
                Operand.Constant(4)
            )),
            (Instruction.BL("printf")),
            (Instruction.MOV(Operand.Register(0), Operand.Constant(0))),
            (Instruction.BL("fflush")),
            (Instruction.POP(arrayListOf(Operand.Pc)))
        )
    )
}

fun CodeGenerator.add_pPrintBool(tagValue: Int) {
    // This should be called at the end of the program after checking the flags
    // The required messages for this:
    //                      true\0 resides at tagValue - 1 ( = messageCounter - 2 )
    //                      false\0 resides at tagValue ( = messageCounter - 1 )
    instructions.addAll(
        arrayListOf(
            Instruction.LABEL("p_print_bool"),
            Instruction.CMP(Operand.Register(0), Operand.Constant(0)),
            Instruction.LDRCond(Operand.Register(0), Operand.MessageTag(tagValue - 1), "NE"),
            Instruction.LDRCond(Operand.Register(0), Operand.MessageTag(tagValue), "EQ"),
            Instruction.ADD(
                Operand.Register(0),
                Operand.Register(0),
                Operand.Constant(4)
            ),
            Instruction.BL("printf"),
            Instruction.MOV(Operand.Register(0), Operand.Constant(0)),
            Instruction.BL("fflush"),
            Instruction.POP(arrayListOf(Operand.Pc))
        )
    )
}

fun CodeGenerator.add_pPrintInt(tagValue: Int) {
    // This should be called at the end of the program after checking the flags
    // The required messages for this: %d\0 resides at tagValue ( = messageCounter - 1 )
    instructions.addAll(
        arrayListOf(
            Instruction.LABEL("p_print_int"),
            Instruction.PUSH(arrayListOf(Operand.Lr)),
            Instruction.MOV(Operand.Register(1), Operand.Register(0)),
            Instruction.LDRSimple(Operand.Register(0), Operand.MessageTag(tagValue)),
            Instruction.ADD(
                Operand.Register(0),
                Operand.Register(0),
                Operand.Constant(4)
            ),
            Instruction.BL("printf"),
            Instruction.MOV(Operand.Register(0), Operand.Constant(0)),
            Instruction.BL("fflush"),
            Instruction.POP(arrayListOf(Operand.Pc))
        )
    )
}

fun CodeGenerator.add_pPrintReference(tagValue: Int) {
    // This should be called at the end of the program after checking the flags
    // The required messages for this : %p\0 resides at tagValue ( = messageCounter - 1 )
    instructions.addAll(
        arrayListOf(
            Instruction.LABEL("p_print_reference"),
            Instruction.PUSH(arrayListOf(Operand.Lr)),
            Instruction.LDRSimple(Operand.Register(0), Operand.MessageTag(tagValue)),
            Instruction.ADD(
                Operand.Register(0),
                Operand.Register(0),
                Operand.Constant(4)
            ),
            Instruction.BL("printf"),
            Instruction.MOV(Operand.Register(0), Operand.Constant(0)),
            Instruction.BL("fflush"),
            Instruction.POP(arrayListOf(Operand.Pc))
        )
    )
}

fun CodeGenerator.add_pPrintLn(tagValue: Int) {
    // This should be called at the end of the program after checking the flags
    // The required messages for this : \0 resides at tagValue ( = messageCounter - 1 )
    instructions.addAll(
        arrayListOf(
            Instruction.LABEL("p_print_ln"),
            Instruction.PUSH(arrayListOf(Operand.Lr)),
            Instruction.LDRSimple(Operand.Register(0), Operand.MessageTag(tagValue)),
            Instruction.ADD(
                Operand.Register(0),
                Operand.Register(0),
                Operand.Constant(4)
            ),
            Instruction.BL("puts"),
            Instruction.MOV(Operand.Register(0), Operand.Constant(0)),
            Instruction.BL("fflush"),
            Instruction.POP(arrayListOf(Operand.Pc))
        )
    )
}



fun CodeGenerator.printTypeInstructions(expression: Expression) {
    when {
        Type.compare(expression.exprType, Type.TArray(Type.TAny)) ||
                Type.compare(expression.exprType, Type.TPair(Type.TAny, Type.TAny)) -> {
            printReference = true
            instructions.add(Instruction.BL("p_print_reference"))
        }

        Type.compare(expression.exprType, Type.TChar) -> {
            instructions.add(Instruction.BL("putchar"))

        }

        Type.compare(expression.exprType, Type.TString) -> {
            printString = true
            messageTagGenerator((expression as Expression.Literal.LString).string)
            // TODO: check here about what happens because message generator is called here so the tag
            // TODO: is generated here but it has already been passed through compileExpression so maybe
            // TODO: the function call to messageTagGenerator should be in compileExpression
            // TODO: but what if strings are used elsewhere?
            instructions.add(Instruction.BL("p_print_string"))
        }

        Type.compare(expression.exprType, Type.TInt) -> {
            printInt = true
            instructions.add(Instruction.BL("p_print_int"))
        }

        Type.compare(expression.exprType, Type.TBool) -> {
            printBool = true
            instructions.add(Instruction.BL("p_print_bool"))
        }
    }
}