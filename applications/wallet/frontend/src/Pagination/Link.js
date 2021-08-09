import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing, constants } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

import { getQueryString } from '../Fetch/helpers'

import Button, { VARIANTS } from '../Button'

const PaginationLink = ({ currentPage, totalPages, direction }) => {
  const { pathname, query } = useRouter()

  const isPrev = direction === 'prev'

  const isDisabled = isPrev ? currentPage - 1 <= 0 : currentPage === totalPages

  const queryParamPage = isPrev ? currentPage - 1 : currentPage + 1
  const queryParam = getQueryString({
    query: query.query,
    ordering: query.sort,
    search: query.search,
    filters: query.filters,
    page: queryParamPage === 1 ? '' : queryParamPage,
  })
  const href = `${pathname}${queryParam}`
  const as = href
    .split('/')
    .map((s) => s.replace(/\[(.*)\]/gi, (_, group) => query[group]))
    .join('/')

  return (
    <Link href={href} as={as} passHref>
      <Button
        aria-label={isPrev ? 'Previous page' : 'Next page'}
        title={isPrev ? 'Previous page' : 'Next page'}
        variant={VARIANTS.SECONDARY_SMALL}
        style={{ padding: spacing.moderate }}
        rel={direction}
        isDisabled={isDisabled}
      >
        <ChevronSvg
          height={constants.icons.mini}
          css={{ transform: `rotate(${isPrev ? '' : '-'}90deg)` }}
        />
      </Button>
    </Link>
  )
}

PaginationLink.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
  direction: PropTypes.oneOf(['prev', 'next']).isRequired,
}

export default PaginationLink
