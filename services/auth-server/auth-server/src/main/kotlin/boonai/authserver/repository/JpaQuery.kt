package boonai.authserver.repository

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import javax.persistence.EntityManager
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

class JpaQuery<T> (
    entityManager: EntityManager,
    private val filter: AbstractJpaFilter<T>,
    private val type: Class<T>
) {

    private val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder

    fun getQuery(): CriteriaQuery<T> {
        val query = criteriaBuilder.createQuery(type)
        val root = query.from(type)

        query.select(root)
        query.where(*filter.buildWhereClause(root, criteriaBuilder))
        applySort(query, root)
        return query
    }

    fun getQueryForCount(): CriteriaQuery<Long> {
        val query = criteriaBuilder.createQuery(Long::class.java)
        val root = query.from(type)

        query.select(criteriaBuilder.count(root))
        query.where(*filter.buildWhereClause(root, criteriaBuilder))
        return query
    }

    fun applySort(query: CriteriaQuery<T>, root: Root<T>) {
        query.orderBy(
            filter.sort
                .filter { it.substring(0, it.indexOf(':')) in filter.sortFields }
                .map {
                    val (col, dir) = it.split(":")
                    if (dir.startsWith("a", ignoreCase = true)) {
                        criteriaBuilder.asc(root.get<T>(col))
                    } else {
                        criteriaBuilder.desc(root.get<T>(col))
                    }
                }
        )
    }
}

@ApiModel("PageList", description = "A list with some paging properies.")
class PagedList<T> (
    @ApiModelProperty("The page that was returned")
    val page: Page,
    @ApiModelProperty("A list of results.")
    val list: List<T>
)

@ApiModel("Page", description = "The page setttings for a search request.")
class Page(
    @ApiModelProperty("The from parameter defines the offset from the first result. ")
    val from: Int,
    @ApiModelProperty("The number of results to return ")
    val size: Int,
    @ApiModelProperty("The total number of results for all pages.   ")
    val totalCount: Long = 0
) {
    fun withTotal(total: Long): Page {
        return Page(from, size, total)
    }
}

abstract class AbstractJpaFilter<T> {

    abstract val sortFields: Set<String>

    var page: Page = Page(0, 50)

    var sort: List<String> = listOf("id:desc")

    abstract fun buildWhereClause(root: Root<T>, cb: CriteriaBuilder): Array<Predicate>
}
