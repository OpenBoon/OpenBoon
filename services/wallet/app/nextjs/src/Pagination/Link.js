import React from 'react'
import PropTypes from 'prop-types'

import { spacing, constants, colors } from '../Styles'

import ChevronSvg from './chevron.svg'

const DEFAULT_PADDING = `${spacing.cozy}px ${spacing.moderate}px`
const BORDER_RADIUS = constants.borderRadius.small

const PaginationLink = ({
  currentPage,
  totalPages,
  direction,
  href,
  onClick,
}) => {
  const isPrev = direction === 'prev'

  const isDisabled = isPrev
    ? currentPage - 1 <= 0
    : currentPage + 1 > totalPages

  const Element = isDisabled ? 'button' : 'a'

  const addedProps = isDisabled
    ? { disabled: isDisabled }
    : {
        href,
        rel: direction,
        onClick: onClick({
          page: isPrev ? currentPage - 1 : currentPage + 1,
        }),
      }

  return (
    <Element
      css={{
        display: 'flex',
        alignItems: 'center',
        padding: DEFAULT_PADDING,
        backgroundColor: colors.grey5,
        border: constants.borders.default,
        borderTopLeftRadius: isPrev ? BORDER_RADIUS : 0,
        borderBottomLeftRadius: isPrev ? BORDER_RADIUS : 0,
        borderTopRightRadius: direction === 'next' ? BORDER_RADIUS : 0,
        borderBottomRightRadius: direction === 'next' ? BORDER_RADIUS : 0,
        '&:hover': {
          opacity: isDisabled ? 1 : constants.opacity.half,
          textDecoration: 'none',
        },
      }}
      // eslint-disable-next-line react/jsx-props-no-spreading
      {...addedProps}>
      <ChevronSvg
        width={16}
        css={{
          color: colors.grey2,
          transform: `rotate(${isPrev ? '' : '-'}90deg)`,
        }}
      />
    </Element>
  )
}

PaginationLink.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
  direction: PropTypes.oneOf(['prev', 'next']).isRequired,
  href: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
}

export default PaginationLink
