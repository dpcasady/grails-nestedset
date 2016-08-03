package com.cmt.nestedset

import com.cmt.nestedset.ast.Nestedset
import grails.persistence.Entity
import spock.lang.Specification
import spock.lang.Unroll

class NestedSetASTAnnotationSpec extends Specification {

    NestedSetDomain domain = new NestedSetDomain()

    void "NestedsetDomain implements NestedsetTrait"() {
        expect:
        domain instanceof NestedsetTrait
    }

    @Unroll
    void "NestedsetDomain has property '#property'"() {
        expect:
        domain.hasProperty(property)

        where:
        property           | _
        'lft'              | _
        'rgt'              | _
        'depth'            | _
        'parent'           | _
        'nestedsetMutable' | _
    }

    @Unroll
    void "NestedsetDomain responds to '#method'"() {
        expect:
        domain.respondsTo(method)

        where:
        method             | _
        'isLeaf'           | _
        'isRootNode'       | _
        'getTree'          | _
        'getDescendants'   | _
        'countDescendants' | _
        'getLeafs'         | _
        'getAncestors'     | _
        'getChildren'      | _
        'countChildren'    | _
        'getLastChild'     | _
        'getRoot'          | _
        'isDescendant'     | _
    }
}

@Entity
@Nestedset
class NestedSetDomain { }
