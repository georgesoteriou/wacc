package uk.ac.ic.doc.wacc

import uk.ac.ic.doc.wacc.assembly_code.Instruction
import uk.ac.ic.doc.wacc.assembly_code.Operand
import uk.ac.ic.doc.wacc.ast.*
import java.lang.IllegalArgumentException

class CodeGenerator(var program: Program) {

    var labelCounter = 0
    var instructions: MutableList<Instruction> = arrayListOf()
    var data: MutableList<Instruction> = arrayListOf()
    var activeScope = ActiveScope(Scope(), null)
    var messageCounter = 0
    var printString = false
    var printInt = false
    var printBool = false
    var printLn = false
    var printReference = false
    fun compile() {
        instructions.add(Instruction.Flag(".global main"))
        //TODO: Add functions to active scope
        compileStatement(program.block, "main")
        instructions.add(Instruction.Flag(".ltorg"))
        instructions.forEach { println(it.toString()) }
    }

    fun compileStatement(statement: Statement, name: String = ".L$labelCounter") {
        when (statement) {
            is Statement.Block -> {
                statement.scope.findFullSize()
                instructions.add(Instruction.LABEL(name))
                instructions.add(Instruction.PUSH(arrayListOf(Operand.Lr)))
                activeScope = activeScope.newSubScope(statement.scope)
                decreaseSP(statement)
                statement.statements.forEach { compileStatement(it) }
                labelCounter++
                // TODO add later: increment label counter : if name not like ".L<Int>"
                increaseSP(statement)
                instructions.add(Instruction.LDRSimple(Operand.Register(0), Operand.Literal.LInt("0")))
                instructions.add(Instruction.POP(arrayListOf(Operand.Pc)))
            }
            is Statement.Skip -> {
            }
            is Statement.VariableDeclaration -> {
                var type = statement.lhs.type
                val name = statement.lhs.name
                when (type) {
                    is Type.TInt -> {
                        val value = (statement.rhs as Expression.Literal.LInt).int
                        intAssignInstructions(name, value)
                    }
                    is Type.TBool -> {
                        val value = (statement.rhs as Expression.Literal.LBool).bool
                        boolAssignInstructions(name, value)
                    }
                    is Type.TChar -> {
                        val value = (statement.rhs as Expression.Literal.LChar).char
                        charAssignInstructions(name, value)
                    }
                    is Type.TArray -> {
                        arrayDeclInstructions(statement)
                    }
                    is Type.TPair -> {
                        pairDeclInstructions(statement)
                        //TODO: deal with null pairs
                    }
                }
                activeScope.declare(name)

            }

            is Statement.VariableAssignment -> {
                var type = statement.rhs.exprType
                val name = (statement.lhs as Expression.Identifier).name
                when (type) {
                    is Type.TInt -> {
                        val value = (statement.rhs as Expression.Literal.LInt).int
                        intAssignInstructions(name, value)
                    }
                    is Type.TBool -> {
                        val value = (statement.rhs as Expression.Literal.LBool).bool
                        boolAssignInstructions(name, value)
                    }
                    is Type.TChar -> {
                        val value = (statement.rhs as Expression.Literal.LChar).char
                        charAssignInstructions(name, value)
                    }
                    is Type.TArray -> {
                     //   arrayDeclInstructions(statement)
                    }
                    is Type.TPair -> {
                       // pairDeclInstructions(statement)
                        //TODO: deal with null pairs
                    }
                }
            }
            is Statement.ReadInput -> {
            }
            is Statement.FreeVariable -> {
            }
            is Statement.Return -> {
            }
            is Statement.Exit -> {
                compileExpression(statement.expression)
                instructions.add(Instruction.MOV(Operand.Register(0), Operand.Register(4)))
                instructions.add(Instruction.BL("exit"))
            }
            is Statement.Print -> {
                compileExpression(statement.expression)
                instructions.add(Instruction.MOV(Operand.Register(0), Operand.Register(4)))
                when {
                    Type.compare(statement.expression.exprType,Type.TArray(Type.TAny)) ||
                            Type.compare(statement.expression.exprType,Type.TPair(Type.TAny,Type.TAny))
                    -> {
                        printReference = true
                        instructions.add(Instruction.BL("p_print_int"))
                    }


                    Type.compare(statement.expression.exprType,Type.TChar) -> {
                        instructions.add(Instruction.BL("putchar"))

                    }
                    Type.compare(statement.expression.exprType,Type.TString)
                    -> {
                        printString = true
                        // TODO : Message tag generator call here
                        statement.expression as Type.TString
                        instructions.add(Instruction.BL("p_print_string"))
                    }

                    Type.compare(statement.expression.exprType,Type.TInt) -> {
                        printInt = true
                        instructions.add(Instruction.BL("p_print_int"))
                    }

                    Type.compare(statement.expression.exprType,Type.TBool) -> {
                        printBool = true
                        instructions.add(Instruction.BL("p_print_bool"))
                    }
                }
            }
            is Statement.PrintLn -> {
            }
            is Statement.If -> {
            }
            is Statement.While -> {
            }
        }

    }

    private fun pairDeclInstructions(statement: Statement.VariableDeclaration) {
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
        pairElemDeclInstructions(typeL, e1)

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

        pairElemDeclInstructions(typeR, e2)

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

    private fun pairElemDeclInstructions(type: Type, expr: Expression) {
        when (type) {
            is Type.TArray -> {
                addPointerLDR(expr)
            }
            is Type.TPair -> {
                addPointerLDR(expr)
            }
            else -> {
                instructions.add(
                    Instruction.LDRSimple(
                        Operand.Register(5),
                        when (type) {
                            is Type.TInt -> Operand.Literal.LInt((expr as Expression.Literal.LInt).int)
                            is Type.TBool -> Operand.Literal.LBool((expr as Expression.Literal.LBool).bool)
                            is Type.TChar -> Operand.Literal.LChar((expr as Expression.Literal.LChar).char)
                            else -> throw IllegalArgumentException()
                        }
                    )
                )
            }
        }
    }

    private fun arrayDeclInstructions(statement: Statement.VariableDeclaration) {
        instructions.add(
            Instruction.LDRSimple(
                Operand.Register(0),
                Operand.Literal.LInt(
                    (((statement.rhs as Expression.Literal.LArray).params.size) *
                            Type.size(statement.lhs.type) + 4).toString()
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
        var offset = Type.size(statement.lhs.type)
        var type = (statement.rhs.exprType as Type.TArray).type
        (statement.rhs as Expression.Literal.LArray).params.forEach {
            when (type) {
                is Type.TArray -> {
                    addPointerLDR(it)
                    instructions.add(
                        Instruction.STROffset(
                            Operand.Register(5),
                            Operand.Register(4),
                            Operand.Offset(offset)
                        )
                    )
                }
                is Type.TPair -> {
                    addPointerLDR(it)
                    instructions.add(
                        Instruction.STROffset(
                            Operand.Register(5),
                            Operand.Register(4),
                            Operand.Offset(offset)
                        )
                    )
                }
                else -> {
                    instructions.add(
                        Instruction.LDRSimple(
                            Operand.Register(5),
                            when (type) {
                                is Type.TInt -> Operand.Literal.LInt((it as Expression.Literal.LInt).int)
                                is Type.TBool -> Operand.Literal.LBool((it as Expression.Literal.LBool).bool)
                                is Type.TChar -> Operand.Literal.LChar((it as Expression.Literal.LChar).char)
                                else -> throw IllegalArgumentException()
                            }
                        )
                    )

                    instructions.add(
                        Instruction.STROffset(
                            Operand.Register(5),
                            Operand.Register(4),
                            Operand.Offset(offset)
                        )
                    )
                }
            }
            offset += Type.size(statement.lhs.type)
        }

        //Storing no. of array elems
        instructions.add(
            Instruction.LDRSimple(
                Operand.Register(5),
                Operand.Literal.LInt((statement.rhs as Expression.Literal.LArray).params.size.toString())
            )
        )
        instructions.add(
            Instruction.STRSimple(
                Operand.Register(5),
                Operand.Register(4)
            )
        )
        //Store array to sp
        var pos = activeScope.getPosition(statement.lhs.name)
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

    private fun charAssignInstructions(name: String, value: Char) {
        instructions.add(
            Instruction.MOV(
                Operand.Register(4),
                Operand.Literal.LChar(value)
            )
        )
        val pos = activeScope.getPosition(name)
        instructions.add(
            Instruction.STRB(
                Operand.Register(4),
                Operand.Sp,
                Operand.Offset(pos)
            )
        )
    }

    private fun boolAssignInstructions(name: String, value: Boolean) {
        instructions.add(
            Instruction.MOV(
                Operand.Register(4),
                Operand.Literal.LBool(value)
            )
        )

        val pos = activeScope.getPosition(name)
        instructions.add(
            Instruction.STRB(
                Operand.Register(4),
                Operand.Sp,
                Operand.Offset(pos)
            )
        )
    }

    private fun intAssignInstructions(name: String, value: String) {
        instructions.add(
            Instruction.LDRSimple(
                Operand.Register(4),
                Operand.Literal.LInt(value)
            )
        )
        var pos = activeScope.getPosition(name)
        if (pos != 0) {
            instructions.add(
                Instruction.STROffset(
                    Operand.Register(4),
                    Operand.Sp,
                    Operand.Offset(pos)
                )
            )
        } else {
            instructions.add(
                Instruction.STRSimple(
                    Operand.Register(4),
                    Operand.Sp
                )
            )
        }
    }

    private fun addPointerLDR(e1: Expression) {
        var pos = activeScope.getPosition((e1 as Expression.Identifier).name)
        instructions.add(
            Instruction.LDRRegister(
                Operand.Register(5),
                Operand.Sp,
                Operand.Offset(pos)
            )
        )
    }

    private fun increaseSP(statement: Statement.Block) {
        var declarationsSize = statement.scope.fullSize
        for (i in 1..statement.scope.fullSize step 1024) {
            instructions.add(
                Instruction.ADD(
                    Operand.Sp, Operand.Sp, Operand.Offset(
                        if (declarationsSize > 1024) {
                            declarationsSize -= 1024
                            1024
                        } else {
                            declarationsSize
                        }
                    )
                )
            )
        }
    }

    private fun decreaseSP(statement: Statement.Block): Int {
        var declarationsSize = statement.scope.fullSize
        for (i in 1..statement.scope.fullSize step 1024) {
            instructions.add(
                Instruction.SUB(
                    Operand.Sp, Operand.Sp, Operand.Offset(
                        if (declarationsSize > 1024) {
                            declarationsSize -= 1024
                            1024
                        } else {
                            declarationsSize
                        }
                    )
                )
            )
        }
        return declarationsSize
    }

    fun compileExpression(expression: Expression) {
        when (expression) {
            is Expression.CallFunction -> {
            }
            is Expression.NewPair -> {
            }
            is Expression.Identifier -> {
            }

            is Expression.Literal.LInt -> {
                instructions.add(
                    Instruction.LDRSimple(
                        Operand.Register(4),
                        Operand.Literal.LInt(expression.int)
                    )
                )
            }
            is Expression.Literal.LBool -> {
                instructions.add(
                    Instruction.LDRSimple(
                        Operand.Register(4),
                        Operand.Literal.LBool(expression.bool)
                    )
                )
            }
            is Expression.Literal.LChar -> {
                instructions.add(
                    Instruction.LDRSimple(
                        Operand.Register(4),
                        Operand.Literal.LChar(expression.char)
                    )
                )
            }
            is Expression.Literal.LString -> {
            }
            is Expression.Literal.LArray -> {
            }
            is Expression.Literal.LPair -> {
            }

            is Expression.BinaryOperation -> {
            }
            is Expression.UnaryOperation -> {
                when (expression.operator) {
                    Expression.UnaryOperator.MINUS -> {
                        instructions.add(
                            Instruction.LDRSimple(
                                Operand.Register(4),
                                Operand.Literal.LInt(
                                    "-${(expression.expression as Expression.Literal.LInt).int}"
                                )
                            )
                        )
                    }
                    Expression.UnaryOperator.CHR,
                    Expression.UnaryOperator.LEN,
                    Expression.UnaryOperator.NOT,
                    Expression.UnaryOperator.ORD -> {
                    }
                }
            }

            is Expression.ArrayElem -> {
            }
            is Expression.Fst -> {
            }
            is Expression.Snd -> {
            }
        }
    }


}