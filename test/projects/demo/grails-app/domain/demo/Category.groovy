package demo

import com.ticketbis.nestedset.ast.Nestedset
import grails.compiler.GrailsCompileStatic
import groovy.transform.ToString

@GrailsCompileStatic
@Nestedset
@ToString(includeNames=true)
class Category {
    String name

    Date lastUpdated

    static constraints = {

    }

    static mapping = {
    }

    static namedQueries = {
    }

}
