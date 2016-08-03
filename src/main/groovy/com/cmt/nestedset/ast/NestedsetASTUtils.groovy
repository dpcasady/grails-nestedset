package com.cmt.nestedset.ast

import grails.core.GrailsDomainClassProperty
import groovy.transform.PackageScope
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.grails.compiler.injection.GrailsASTUtils

@PackageScope
@CompileStatic
class NestedSetASTUtils {

    static FieldNode getOrCreateField(ClassNode classNode, String fieldName, Expression initialExpression, int modifiers = FieldNode.ACC_PUBLIC, Class fieldType = Object) {
        getOrCreateField(classNode, fieldName, initialExpression, modifiers, new ClassNode(fieldType))
    }

    static FieldNode getOrCreateField(ClassNode classNode, String fieldName, Expression initialExpression, int modifiers = FieldNode.ACC_PUBLIC, ClassNode fieldType) {
        if (!classNode.getDeclaredField(fieldName)) {
            FieldNode field = new FieldNode(fieldName, modifiers, fieldType, classNode, initialExpression)

            field.declaringClass = classNode
            classNode.addField(field)
            return field
        }
        classNode.getDeclaredField(fieldName)
    }

    static FieldNode getOrCreateProperty(ClassNode classNode, String fieldName, Expression initialExpression, int modifiers, ClassNode fieldType) {
        if (!classNode.getDeclaredField(fieldName)) {
            classNode.addProperty(fieldName, modifiers, fieldType, initialExpression, null, null)
        }
        classNode.getDeclaredField(fieldName)
    }

    static FieldNode getOrCreateStaticField(ClassNode classNode, String fieldName, Expression initialExpression, int modifiers = FieldNode.ACC_PUBLIC, Class fieldType = Object) {
        getOrCreateField(classNode, fieldName, initialExpression, modifiers | FieldNode.ACC_STATIC, fieldType)
    }

    static ListExpression getTransientsListExpression(ClassNode classNode) {
        FieldNode transientsField = getOrCreateStaticField(classNode, GrailsDomainClassProperty.TRANSIENT, new ListExpression())

        (ListExpression) transientsField.initialExpression
    }

    static MapExpression getHasManyMapExpression(ClassNode classNode) {
        FieldNode transientsField = getOrCreateStaticField(classNode, GrailsDomainClassProperty.HAS_MANY, new MapExpression())

        (MapExpression) transientsField.initialExpression
    }

    static ClosureExpression getNamedQueriesClosureExpression(ClassNode classNode) {
        FieldNode namedQueriesField = getOrCreateStaticField(classNode, GrailsDomainClassProperty.NAMED_QUERIES, new ClosureExpression(GrailsASTUtils.ZERO_PARAMETERS, new BlockStatement()))

        (ClosureExpression) namedQueriesField.initialExpression
    }

    static void addTransient(ClassNode classNode, String fieldName) {
        ListExpression transientsListExpr = getTransientsListExpression(classNode)
        transientsListExpr.addExpression(new ConstantExpression(fieldName))
    }

    static void addHasManyRelationship(ClassNode classNode, String relName, ClassNode relClass) {
        MapExpression hasManyMapExpr = getHasManyMapExpression(classNode)
        hasManyMapExpr.addMapEntryExpression(new ConstantExpression(relName), new ClassExpression(relClass))
    }

    static void addNamedQuery(ClassNode classNode, Statement code) {
        ClosureExpression namedQueriesExpr = getNamedQueriesClosureExpression(classNode)
        BlockStatement blockStatement = (BlockStatement) namedQueriesExpr.code
        blockStatement.addStatement(code)
    }

    static void addSettings(String name, ClassNode classNode, String fieldName, String config) {
        if (config == null)
            return

        String configStr = fieldName + " " + config

        BlockStatement newConfig = (BlockStatement) new AstBuilder().buildFromString(configStr).get(0)

        FieldNode closure = classNode.getField(name)
        if (closure == null) {
            createStaticClosure(classNode, name)
            closure = classNode.getField(name)
            assert closure != null
        }

        if (!hasFieldInClosure(closure, fieldName)) {
            ReturnStatement returnStatement = (ReturnStatement) newConfig.getStatements().get(0)
            ExpressionStatement expressionStatement = new ExpressionStatement(returnStatement.getExpression())
            ClosureExpression exp = (ClosureExpression) closure.getInitialExpression()
            BlockStatement block = (BlockStatement) exp.getCode()
            block.addStatement(expressionStatement)
        }

        assert hasFieldInClosure(closure,fieldName) == true
    }

    static void createStaticClosure(ClassNode classNode,String name) {
        FieldNode field = new FieldNode(name, FieldNode.ACC_PUBLIC | FieldNode.ACC_STATIC, new ClassNode(java.lang.Object.class), new ClassNode(classNode.getClass()), null)
        ClosureExpression expression = new ClosureExpression(Parameter.EMPTY_ARRAY, new BlockStatement())
        expression.setVariableScope(new VariableScope())
        field.setInitialValueExpression(expression)
        classNode.addField(field)
    }

    static boolean hasFieldInClosure(FieldNode closure, String fieldName) {
        if (closure != null) {
            ClosureExpression expression = (ClosureExpression) closure.getInitialExpression()
            BlockStatement block = (BlockStatement) expression.getCode()
            List<Statement> statements = block.getStatements()
            for (Statement statement : statements) {
                if (statement instanceof ExpressionStatement && ((ExpressionStatement)statement).getExpression() instanceof MethodCallExpression) {
                    ExpressionStatement expressionStatement = (ExpressionStatement) statement
                    MethodCallExpression methodCall = (MethodCallExpression) expressionStatement.getExpression()
                    ConstantExpression constantExpression = (ConstantExpression) methodCall.getMethod()
                    if (constantExpression.getValue().equals(fieldName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

}
