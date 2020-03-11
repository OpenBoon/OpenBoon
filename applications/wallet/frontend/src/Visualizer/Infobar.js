import PropTypes from 'prop-types'

import { constants, colors, spacing } from '../Styles'

import VisualizerPagination from './Pagination'

const PER_PAGE_LIMIT = 50

const VisualizerInfobar = ({ currentPage, totalCount }) => {
  return (
    <div
      css={{
        minHeight: constants.navbar.height,
        display: 'flex',
        alignItems: 'center',
        backgroundColor: colors.structure.mattGrey,
        boxShadow: constants.boxShadows.infoBar,
        fontFamily: 'Roboto Condensed',
        padding: `0 ${spacing.normal}px`,
      }}>
      <div
        css={{ color: colors.structure.steel, padding: `${spacing.base} 0` }}>
        Sort: Import Date
      </div>

      <div css={{ flex: 1 }} />

      <div
        css={{
          color: colors.structure.zinc,
          padding: spacing.base,
          paddingRight: spacing.normal,
        }}>
        {totalCount} Results
      </div>
      <div css={{ color: colors.structure.steel }}>|</div>
      <VisualizerPagination
        currentPage={currentPage}
        totalPages={Math.ceil(totalCount / PER_PAGE_LIMIT)}
      />
    </div>
  )
}

VisualizerInfobar.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalCount: PropTypes.number.isRequired,
}

export default VisualizerInfobar
