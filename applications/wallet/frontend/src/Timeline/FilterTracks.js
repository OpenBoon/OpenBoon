import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'

import { TICK_WIDTH } from './helpers'
import { ACTIONS } from './reducer'

const OFFSET = (TICK_WIDTH + constants.borderWidths.regular) / 2

const TimelineFilterTracks = ({ width, filter, dispatch }) => {
  return (
    <div
      css={{
        marginLeft: -width,
        width,
        paddingRight: OFFSET,
        marginBottom: -spacing.hairline,
        zIndex: zIndex.timeline.tracks,
      }}
    >
      <InputSearch
        aria-label="Filter tracks"
        placeholder="Filter tracks"
        value={filter}
        onChange={({ value }) => {
          dispatch({ type: ACTIONS.UPDATE_FILTER, payload: { value } })
        }}
        variant={INPUT_SEARCH_VARIANTS.DARK}
        style={{
          height: '100%',
          paddingTop: 0,
          paddingBottom: 0,
          color: colors.structure.zinc,
          backgroundColor: colors.structure.coal,
        }}
      />
    </div>
  )
}

TimelineFilterTracks.propTypes = {
  width: PropTypes.number.isRequired,
  filter: PropTypes.string.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineFilterTracks
