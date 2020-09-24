import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'

import { ACTIONS } from './reducer'

const TimelineFilterTracks = ({ settings, dispatch }) => {
  return (
    <div
      css={{
        marginLeft: -constants.timeline.modulesWidth,
        width: constants.timeline.modulesWidth,
      }}
    >
      <InputSearch
        aria-label="Filter tracks"
        placeholder="Filter tracks"
        value={settings.filter}
        onChange={({ value }) => {
          dispatch({ type: ACTIONS.UPDATE_FILTER, payload: { value } })
        }}
        variant={INPUT_SEARCH_VARIANTS.DARK}
        style={{
          height: '100%',
          paddingTop: 0,
          paddingBottom: 0,
          color: colors.structure.zinc,
          backgroundColor: colors.structure.transparent,
        }}
      />
    </div>
  )
}

TimelineFilterTracks.propTypes = {
  settings: PropTypes.shape({
    filter: PropTypes.string.isRequired,
    modules: PropTypes.shape({}).isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default TimelineFilterTracks
