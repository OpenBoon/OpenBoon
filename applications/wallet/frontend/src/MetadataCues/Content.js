import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import MetadataPrettyPredictionsContent from '../MetadataPretty/PredictionsContent'

const COLOR_WIDTH = 3

const COLORS = [
  colors.signal.sky.base,
  colors.graph.magenta,
  colors.signal.halloween.base,
  colors.signal.canary.base,
  colors.graph.seafoam,
  colors.graph.rust,
  colors.graph.coral,
  colors.graph.iris,
  colors.graph.marigold,
  colors.graph.magenta,
  colors.signal.grass.base,
]

const MetadataCuesContent = ({ metadata, height }) => {
  return (
    <div
      css={{
        height,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
      <div
        css={{
          padding: spacing.base,
          paddingLeft: spacing.moderate,
          borderBottom: constants.borders.regular.smoke,
        }}
      >
        Time Based Metadata:
      </div>

      <div css={{ flex: 1, overflowY: 'auto' }}>
        {Object.keys(metadata).length === 0 && (
          <div
            css={{
              padding: spacing.normal,
              color: colors.structure.white,
              fontStyle: typography.style.italic,
            }}
          >
            No predictions have been detected in this asset at this time mark.
            Try scrubbing forward or backward.
          </div>
        )}

        {Object.entries(metadata).map(([module, predictions], index) => {
          const colorIndex = index % COLORS.length

          return (
            <div
              key={module}
              css={{
                borderBottom: constants.borders.large.smoke,
                boxShadow: `inset ${COLOR_WIDTH}px 0 0 ${COLORS[colorIndex]}`,
              }}
            >
              <MetadataPrettyPredictionsContent
                name={module}
                predictions={predictions}
              />
            </div>
          )
        })}
      </div>
    </div>
  )
}

MetadataCuesContent.propTypes = {
  metadata: PropTypes.objectOf(
    PropTypes.arrayOf(
      PropTypes.shape({
        label: PropTypes.string.isRequired,
        score: PropTypes.number.isRequired,
      }).isRequired,
    ).isRequired,
  ).isRequired,
  height: PropTypes.number.isRequired,
}

export default MetadataCuesContent
