import React from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing, constants, colors } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

const BORDER_RADIUS = constants.borderRadius.small

const PaginationLink = ({ currentPage, totalPages, direction }) => {
  const { pathname, query } = useRouter()

  const isPrev = direction === 'prev'

  const isDisabled = isPrev ? currentPage - 1 <= 0 : currentPage === totalPages

  const styles = {
    display: 'flex',
    alignItems: 'center',
    padding: `${spacing.base}px ${spacing.moderate}px`,
    backgroundColor: colors.structure.steel,
    border: 'none',
    borderTopLeftRadius: isPrev ? BORDER_RADIUS : 0,
    borderBottomLeftRadius: isPrev ? BORDER_RADIUS : 0,
    borderTopRightRadius: isPrev ? 0 : BORDER_RADIUS,
    borderBottomRightRadius: isPrev ? 0 : BORDER_RADIUS,
    '&:hover': {
      opacity: isDisabled ? 1 : constants.opacity.half,
      textDecoration: 'none',
      cursor: isDisabled ? 'not-allowed' : 'pointer',
    },
  }

  if (isDisabled) {
    return (
      <button type="button" css={styles} disabled>
        <ChevronSvg
          height={constants.icons.mini}
          css={{
            color: colors.structure.coal,
            transform: `rotate(${isPrev ? '' : '-'}90deg)`,
          }}
        />
      </button>
    )
  }

  const queryParamPage = isPrev ? currentPage - 1 : currentPage + 1
  const queryParam = queryParamPage === 1 ? '' : `?page=${queryParamPage}`
  const href = `${pathname}${queryParam}`
  const as = href
    .split('/')
    .map((s) => s.replace(/\[(.*)\]/gi, (_, group) => query[group]))
    .join('/')

  return (
    <Link href={href} as={as} passHref>
      <a css={styles} rel={direction}>
        <ChevronSvg
          height={constants.icons.mini}
          css={{
            color: colors.structure.white,
            transform: `rotate(${isPrev ? '' : '-'}90deg)`,
          }}
        />
      </a>
    </Link>
  )
}

PaginationLink.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
  direction: PropTypes.oneOf(['prev', 'next']).isRequired,
}

export default PaginationLink
