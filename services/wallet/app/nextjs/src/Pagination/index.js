import React from 'react'
import PropTypes from 'prop-types'

import { spacing, constants, colors } from '../Styles'

import PaginationLink from './Link'

const DEFAULT_PADDING = `${spacing.base}px ${spacing.moderate}px`

const Pagination = ({
  legend,
  currentPage,
  totalPages,
  prevLink,
  nextLink,
  onClick,
}) => {
  return (
    <div css={{ display: 'flex', justifyContent: 'flex-end' }}>
      {!!legend && (
        <div
          css={{
            color: colors.grey5,
            padding: DEFAULT_PADDING,
            display: 'flex',
            alignItems: 'center',
          }}>
          {legend}
        </div>
      )}
      <div
        css={{
          color: colors.grey5,
          padding: DEFAULT_PADDING,
          paddingLeft: 0,
          display: 'flex',
          alignItems: 'center',
        }}>
        Page:
      </div>
      <div css={{ display: 'flex' }}>
        <PaginationLink
          currentPage={currentPage}
          totalPages={totalPages}
          direction="prev"
          href={prevLink}
          onClick={onClick}
        />
        <div
          css={{
            padding: DEFAULT_PADDING,
            border: constants.borders.default,
            display: 'flex',
            alignItems: 'center',
            color: colors.grey2,
          }}>
          {currentPage}
        </div>
        <div
          css={{
            padding: DEFAULT_PADDING,
            display: 'flex',
            alignItems: 'center',
            color: colors.grey5,
          }}>
          of {totalPages}
        </div>
        <PaginationLink
          currentPage={currentPage}
          totalPages={totalPages}
          direction="next"
          href={nextLink}
          onClick={onClick}
        />
      </div>
    </div>
  )
}

Pagination.propTypes = {
  legend: PropTypes.string.isRequired,
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
  prevLink: PropTypes.string.isRequired,
  nextLink: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
}

export default Pagination
