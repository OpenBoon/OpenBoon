import React from 'react'
import PropTypes from 'prop-types'

import { spacing, colors } from '../Styles'

import VisualizerPaginationLink from './PaginationLink'

const VisualizerPagination = ({ currentPage, totalPages }) => {
  return (
    <div css={{ display: 'flex', justifyContent: 'flex-end' }}>
      <div
        css={{
          padding: `${spacing.base}px ${spacing.moderate}px`,
          display: 'flex',
          alignItems: 'center',
          color: colors.structure.zinc,
        }}>
        Page {currentPage} of {totalPages}
      </div>
      <VisualizerPaginationLink
        currentPage={currentPage}
        totalPages={totalPages}
        direction="prev"
      />
      <VisualizerPaginationLink
        currentPage={currentPage}
        totalPages={totalPages}
        direction="next"
      />
    </div>
  )
}

VisualizerPagination.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
}

export default VisualizerPagination
