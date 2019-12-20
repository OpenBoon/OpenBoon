import React from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import { spacing, constants, colors } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

const DEFAULT_PADDING = `${spacing.base}px ${spacing.moderate}px`
const BORDER_RADIUS = constants.borderRadius.small

const PaginationLink = ({ currentPage, totalPages, direction, href }) => {
  const isPrev = direction === 'prev'
  const isDisabled = isPrev
    ? currentPage - 1 <= 0
    : currentPage + 1 > totalPages

  const styles = {
    display: 'flex',
    alignItems: 'center',
    padding: DEFAULT_PADDING,
    backgroundColor: colors.grey5,
    border: constants.borders.default,
    borderTopLeftRadius: isPrev ? BORDER_RADIUS : 0,
    borderBottomLeftRadius: isPrev ? BORDER_RADIUS : 0,
    borderTopRightRadius: isPrev ? 0 : BORDER_RADIUS,
    borderBottomRightRadius: isPrev ? 0 : BORDER_RADIUS,
    '&:hover': {
      opacity: isDisabled ? 1 : constants.opacity.half,
      textDecoration: 'none',
    },
  }

  return (
    <>
      {isDisabled ? (
        <button type="button" css={styles} disabled>
          <ChevronSvg
            width={16}
            css={{
              color: colors.grey2,
              transform: `rotate(${isPrev ? '' : '-'}90deg)`,
            }}
          />
        </button>
      ) : (
        <Link href={href} passHref>
          <a css={styles} rel={direction}>
            <ChevronSvg
              width={16}
              css={{
                color: colors.grey2,
                transform: `rotate(${isPrev ? '' : '-'}90deg)`,
              }}
            />
          </a>
        </Link>
      )}
    </>
  )
}

PaginationLink.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
  direction: PropTypes.oneOf(['prev', 'next']).isRequired,
  href: PropTypes.string.isRequired,
}

export default PaginationLink
