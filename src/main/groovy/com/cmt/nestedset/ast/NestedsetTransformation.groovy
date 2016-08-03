package com.cmt.nestedset.ast

import com.cmt.nestedset.NestedSetTrait
import groovy.transform.CompilationUnitAware
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.grails.compiler.injection.GrailsASTUtils

import java.lang.reflect.Modifier

@Slf4j
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class NestedSetTransformation implements ASTTransformation, CompilationUnitAware {

    CompilationUnit compilationUnit

    private static final ClassNode NESTEDSET_NODE = new ClassNode(NestedSetTrait)

    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        assert astNodes[0] instanceof AnnotationNode
        assert astNodes[1] instanceof ClassNode, "@NestedSet can only be applied on classes"

        AnnotationNode annotation = astNodes[0]
        ClassNode targetClassNode = astNodes[1]

        assert GrailsASTUtils.isDomainClass(targetClassNode, sourceUnit), "@NestedSet annotation should be applied over domain classes"

        log.info "Adding NestedSet transform to ${targetClassNode.name}..."

        addProperties(targetClassNode)
        addNestedSetTrait(targetClassNode, sourceUnit)
        addConstraints(targetClassNode)
        addBeforeInsertHook(targetClassNode)
        addBeforeUpdateHook(targetClassNode)
    }

    private void addProperties(ClassNode classNode) {
        ['lft', 'rgt', 'depth'].each { String field ->
            NestedSetASTUtils.getOrCreateProperty(classNode, field, new ConstantExpression(0), Modifier.PUBLIC, ClassHelper.Integer_TYPE)
        }

        // parent node
        // ClassNode classNodeLnk = ClassHelper.make(classNode.name)
        // classNodeLnk.setRedirect(classNode)
        ClassNode classNodeLabel = ClassHelper.makeWithoutCaching(classNode.name)
        NestedSetASTUtils.getOrCreateProperty(classNode, 'parent', new EmptyExpression(), Modifier.PUBLIC, classNodeLabel)

        // transient property to avoid manual nestedset properties manipulation
        NestedSetASTUtils.getOrCreateProperty(classNode, 'nestedsetMutable', new ConstantExpression(false), Modifier.PUBLIC, ClassHelper.Boolean_TYPE)
        NestedSetASTUtils.addTransient(classNode, 'nestedsetMutable')
    }

    private void addNestedSetTrait(ClassNode classNode, SourceUnit sourceUnit) {
        if (classNode.declaresInterface(NESTEDSET_NODE))
            return

        classNode.addInterface(NESTEDSET_NODE)
        org.codehaus.groovy.transform.trait.TraitComposer.doExtendTraits(classNode, sourceUnit, compilationUnit)
    }

    private void addConstraints(ClassNode classNode) {
        NestedSetASTUtils.addSettings('constraints', classNode, 'parent', 'nullable: true')
    }

    /**
     * Add a method call to the b4Insert() method provided by
     * NestedSetTrait in the domain's beforeInsert() GORM event hook.
     */
    private void addBeforeInsertHook(ClassNode classNode) {
        addGormEventHook(classNode, 'Insert')
    }

    /**
     * Add a method call to the b4Update() method provided by
     * NestedSetTrait in the domain's beforeUpdate() GORM event hook.
     */
    private void addBeforeUpdateHook(ClassNode classNode) {
        addGormEventHook(classNode, 'Update')
    }

    private void addGormEventHook(ClassNode classNode, String hook) {
        MethodNode methodNode = classNode.getMethod("before$hook", Parameter.EMPTY_ARRAY)
        if (!methodNode) {
            methodNode = classNode.addMethod("before$hook", Modifier.PUBLIC, ClassHelper.OBJECT_TYPE, Parameter.EMPTY_ARRAY, null, new BlockStatement())
        }

        MethodCallExpression methodCallExpression = new MethodCallExpression(VariableExpression.THIS_EXPRESSION, "b4$hook", MethodCallExpression.NO_ARGUMENTS)
        ExpressionStatement expression = new ExpressionStatement(methodCallExpression)
        Statement[] statements = [expression] as Statement[]
        BlockStatement statement = new BlockStatement(statements, new VariableScope())

        BlockStatement existingStatements = methodNode.code
        existingStatements.addStatement(statement)
    }

}
