package uk.ac.ic.doc.wacc.helpers

import uk.ac.ic.doc.wacc.CodeGenerator
import uk.ac.ic.doc.wacc.assembly_code.Instruction
import uk.ac.ic.doc.wacc.assembly_code.Operand
import uk.ac.ic.doc.wacc.ast.Definition
import uk.ac.ic.doc.wacc.ast.Expression
import uk.ac.ic.doc.wacc.ast.Statement
import uk.ac.ic.doc.wacc.ast.Type


fun CodeGenerator.byteAssignInstructions(name: String) = instructions.add(
    Instruction.STRBOffset(
        Operand.Register(4),
        Operand.Sp,
        Operand.Offset(activeScope.getPosition(name))
    )
)

fun CodeGenerator.wordAssignInstructions(name: String) = instructions.add(
    Instruction.STROffset(
        Operand.Register(4),
        Operand.Sp,
        Operand.Offset(activeScope.getPosition(name))
    )
)


fun CodeGenerator.addPointerLDR(e1: Expression, dest: Int) {
    var pos = activeScope.getPosition((e1 as Expression.Identifier).name)
    instructions.add(
        Instruction.LDRRegister(
            Operand.Register(dest),
            Operand.Sp,
            Operand.Offset(pos)
        )
    )
}

fun CodeGenerator.pairNullDeclInstructions() {
    instructions.add(
        Instruction.LDRSimple(
            Operand.Register(4),
            Operand.Literal.LInt("0")
        )
    )
    instructions.add(
        Instruction.STRSimple(
            Operand.Register(4),
            Operand.Sp
        )
    )
}

fun CodeGenerator.pairDeclInstructions(statement: Statement.VariableDeclaration) {
    instructions.add(
        Instruction.LDRSimple(
            Operand.Register(0),
            Operand.Literal.LInt("8")
        )
    )
    instructions.add(Instruction.BL("malloc"))
    instructions.add(
        Instruction.MOV(
            Operand.Register(4),
            Operand.Register(0)
        )
    )
    var typeL = (statement.rhs.exprType as Type.TPair).t1
    var typeR = (statement.rhs.exprType as Type.TPair).t2
    var e1 = (statement.rhs as Expression.NewPair).e1
    var e2 = (statement.rhs as Expression.NewPair).e2

    elemDeclInstructions(typeL, e1)

    instructions.add(
        Instruction.LDRSimple(
            Operand.Register(0),
            Operand.Literal.LInt(Type.size(e1.exprType).toString())
        )
    )

    instructions.add(Instruction.BL("malloc"))

    instructions.add(
        Instruction.STRSimple(
            Operand.Register(5),
            Operand.Register(0)
        )
    )

    instructions.add(
        Instruction.STRSimple(
            Operand.Register(0),
            Operand.Register(4)
        )
    )

    elemDeclInstructions(typeR, e2)

    instructions.add(
        Instruction.LDRSimple(
            Operand.Register(0),
            Operand.Literal.LInt(Type.size(e1.exprType).toString())
        )
    )

    instructions.add(Instruction.BL("malloc"))

    instructions.add(
        Instruction.STRSimple(
            Operand.Register(5),
            Operand.Register(0)
        )
    )

    instructions.add(
        Instruction.STROffset(
            Operand.Register(0),
            Operand.Register(4),
            Operand.Offset(4)
        )
    )

    instructions.add(
        Instruction.STROffset(
            Operand.Register(4),
            Operand.Sp,
            Operand.Offset(activeScope.getPosition(statement.lhs.name))
        )
    )
}

fun CodeGenerator.elemDeclInstructions(type: Type, expr: Expression) {
    when (type) {
        is Type.TArray, is Type.TPair -> {
            addPointerLDR(expr, 5)
        }
        else -> {
            compileExpression(expr, 5)
        }
    }
}

fun CodeGenerator.arrayAssignInstructions(lhs: Definition, rhs: Expression.Literal.LArray) {
    instructions.add(
        Instruction.LDRSimple(
            Operand.Register(0),
            Operand.Literal.LInt(
                (rhs.params.size * Type.size((lhs.type as Type.TArray).type) + 4).toString()
                // TODO: Fix this for multidimensional arrays & put in expr
            )
        )
    )
    instructions.add(Instruction.BL("malloc"))
    instructions.add(
        Instruction.MOV(
            Operand.Register(4),
            Operand.Register(0)
        )
    )

    var offset = Type.size(lhs.type)
    val type = (rhs.exprType as Type.TArray).type
    rhs.params.forEach {
        elemDeclInstructions(type, it)
        instructions.add(
            Instruction.STROffset(
                Operand.Register(5),
                Operand.Register(4),
                Operand.Offset(offset)
            )
        )
        offset += Type.size(type)
    }

    //Storing no. of array elems
    instructions.add(
        Instruction.LDRSimple(
            Operand.Register(5),
            Operand.Literal.LInt(rhs.params.size.toString())
        )
    )
    instructions.add(
        Instruction.STRSimple(
            Operand.Register(5),
            Operand.Register(4)
        )
    )
    //Store array to sp
    var pos = activeScope.getPosition(lhs.name)
    instructions.add(
        when (pos) {
            0 -> Instruction.STRSimple(
                Operand.Register(4),
                Operand.Sp
            )
            else -> Instruction.STROffset(
                Operand.Register(4),
                Operand.Sp,
                Operand.Offset(pos)
            )
        }
    )
}
