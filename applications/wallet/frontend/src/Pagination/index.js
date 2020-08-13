import React from 'react'
import PropTypes from 'prop-types'

import { spacing, colors } from '../Styles'

import PaginationLink from './Link'

const Pagination = ({ currentPage, totalPages }) => {
  if (totalPages === 1) return null

  return (
    <div css={{ display: 'flex', justifyContent: 'flex-end', flexShrink: 0 }}>
      <PaginationLink
        currentPage={currentPage}
        totalPages={totalPages}
        direction="prev"
      />

      <div
        css={{
          padding: `${spacing.base}px ${spacing.normal}px`,
          display: 'flex',
          alignItems: 'center',
          color: colors.structure.white,
          backgroundColor: colors.structure.mattGrey,
        }}
      >
        {currentPage}
      </div>

      <div
        css={{
          padding: `${spacing.base}px ${spacing.moderate}px`,
          display: 'flex',
          alignItems: 'center',
          color: colors.structure.zinc,
        }}
      >
        of {totalPages}
      </div>

      <PaginationLink
        currentPage={currentPage}
        totalPages={totalPages}
        direction="next"
      />
    </div>
  )
}

Pagination.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
}

export default Pagination
