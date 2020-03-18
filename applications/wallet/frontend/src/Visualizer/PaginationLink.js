import React from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing, constants, colors } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

const VisualizerPaginationLink = ({ currentPage, totalPages, direction }) => {
  const { pathname, query } = useRouter()

  const isPrev = direction === 'prev'

  const isDisabled = isPrev ? currentPage - 1 <= 0 : currentPage === totalPages

  const styles = {
    display: 'flex',
    alignItems: 'center',
    padding: spacing.base,
    border: 'none',
    borderRadius: constants.borderRadius.round,
    backgroundColor: colors.transparent,
    color: isDisabled ? colors.structure.iron : colors.structure.zinc,
    '&:hover': {
      color: colors.structure.white,
      backgroundColor: isDisabled ? colors.transparent : colors.structure.steel,
      textDecoration: 'none',
      cursor: isDisabled ? 'not-allowed' : 'pointer',
    },
  }

  if (isDisabled) {
    return (
      <button type="button" css={styles} disabled>
        <ChevronSvg
          width={20}
          css={{
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
    .map(s => s.replace(/\[(.*)\]/gi, (_, group) => query[group]))
    .join('/')

  return (
    <Link href={href} as={as} passHref>
      <a css={styles} rel={direction}>
        <ChevronSvg
          width={20}
          css={{
            transform: `rotate(${isPrev ? '' : '-'}90deg)`,
          }}
        />
      </a>
    </Link>
  )
}

VisualizerPaginationLink.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
  direction: PropTypes.oneOf(['prev', 'next']).isRequired,
}

export default VisualizerPaginationLink
