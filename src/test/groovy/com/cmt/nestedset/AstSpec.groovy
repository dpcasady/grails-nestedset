package com.cmt.nestedset

import com.cmt.nestedset.ast.Nestedset
import grails.persistence.Entity
import spock.lang.Specification

class AstSpec extends Specification {

    void "Nestedset is applied to a domain"() {
        given: "an @Nestedset annotated domain"
        NestedSetDomain domain = new NestedSetDomain()

        expect: "it to have properties and methods of a Nestedset"
        domain.hasProperty("lft")
        domain.hasProperty("rgt")
        domain.hasProperty("depth")
        domain.hasProperty("parent")
        domain.hasProperty("nestedsetMutable")
        domain.respondsTo("isLeaf")
        domain.respondsTo("isRootNode")
        domain.respondsTo("getTree")
        domain.respondsTo("getDescendants")
    }
}

@Entity
@Nestedset
class NestedSetDomain { }
