import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

const TimelineTracks = ({ name, predictions, isOpen }) => {
  return (
    <div css={{ display: 'flex', flexDirection: 'column' }}>
      <div
        css={{
          padding: spacing.base,
          borderBottom: constants.borders.regular.smoke,
          backgroundColor: colors.structure.soot,
        }}
      >
        {`${name} markers`}
      </div>

      {isOpen &&
        predictions.map(({ label }) => {
          return (
            <div
              key={label}
              css={{
                padding: spacing.base,
                borderBottom: constants.borders.regular.smoke,
              }}
            >
              {`${label} markers`}
            </div>
          )
        })}
    </div>
  )
}

TimelineTracks.propTypes = {
  name: PropTypes.string.isRequired,
  predictions: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
    }),
  ).isRequired,
  isOpen: PropTypes.bool.isRequired,
}

export default TimelineTracks
