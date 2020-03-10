import PropTypes from 'prop-types'

import { constants, colors, spacing } from '../Styles'

import Pagination from '../Pagination'

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
      }}>
      <div css={{ color: colors.structure.steel, padding: spacing.base }}>
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
      <Pagination currentPage={currentPage} totalPages={2} />
    </div>
  )
}

VisualizerInfobar.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalCount: PropTypes.number.isRequired,
}

export default VisualizerInfobar
