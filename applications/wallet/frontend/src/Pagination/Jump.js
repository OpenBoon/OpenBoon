import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing, constants } from '../Styles'

import DoubleChevronSvg from '../Icons/doubleChevron.svg'

import { getQueryString } from '../Fetch/helpers'

import Button, { VARIANTS } from '../Button'

const PaginationJump = ({ currentPage, totalPages, direction }) => {
  const { pathname, query } = useRouter()

  const isPrev = direction === 'prev'

  const isDisabled = isPrev ? currentPage === 1 : currentPage === totalPages

  const queryParam = getQueryString({
    query: query.query,
    sort: query.sort,
    page: isPrev ? '' : totalPages,
  })
  const href = `${pathname}${queryParam}`
  const as = href
    .split('/')
    .map((s) => s.replace(/\[(.*)\]/gi, (_, group) => query[group]))
    .join('/')

  return (
    <Link href={href} as={as} passHref>
      <Button
        aria-label={isPrev ? 'First page' : 'Last page'}
        title={isPrev ? 'First page' : 'Last page'}
        variant={VARIANTS.SECONDARY_SMALL}
        style={{
          padding: spacing.moderate,
          [isPrev ? 'marginRight' : 'marginLeft']: spacing.base,
        }}
        rel={direction}
        isDisabled={isDisabled}
      >
        <DoubleChevronSvg
          height={constants.icons.mini}
          css={{ transform: `rotate(${isPrev ? '' : '-'}90deg)` }}
        />
      </Button>
    </Link>
  )
}

PaginationJump.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
  direction: PropTypes.oneOf(['prev', 'next']).isRequired,
}

export default PaginationJump
