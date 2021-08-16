import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

import DoubleChevronSvg from '../Icons/doubleChevron.svg'

import { useLocalStorage } from '../LocalStorage/helpers'
import { reducer, INITIAL_STATE } from '../Timeline/reducer'

import Button, { VARIANTS } from '../Button'
import MetadataPrettyPredictionsContent from '../MetadataPretty/PredictionsContent'

const COLOR_WIDTH = 3

const MetadataCuesContent = ({ metadata }) => {
  const {
    query: { assetId },
  } = useRouter()

  const [settings] = useLocalStorage({
    key: `TimelineTimelines.${assetId}`,
    reducer,
    initialState: INITIAL_STATE,
  })

  const [, dispatch] = useLocalStorage({
    key: `MetadataCues.${assetId}`,
    reducer: (state, action) => ({ ...state, ...action }),
  })

  return (
    <>
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: spacing.base,
          paddingLeft: spacing.normal,
          backgroundColor: colors.structure.lead,
          textTransform: 'uppercase',
          fontWeight: typography.weight.medium,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
        }}
      >
        <div>Clip Metadata</div>
        <Button
          aria-label="Close Panel"
          variant={VARIANTS.ICON}
          onClick={() => {
            dispatch({ isOpen: false })
          }}
          style={{
            padding: 0,
          }}
        >
          <DoubleChevronSvg
            height={constants.icons.regular}
            css={{
              transform: `rotate(-90deg)`,
            }}
          />
        </Button>
      </div>

      <div css={{ flex: 1, overflowY: 'auto' }}>
        {Object.values(metadata).filter((predictions) => predictions.length > 0)
          .length === 0 && (
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

        {Object.entries(metadata)
          .filter(([, predictions]) => predictions.length > 0)
          .filter(([timeline]) => {
            return settings.timelines[timeline]?.isVisible !== false
          })
          .sort(([a], [b]) => (a > b ? 1 : -1))
          .map(([timeline, predictions]) => {
            return (
              <div
                key={timeline}
                css={{
                  borderBottom: constants.borders.large.smoke,
                  boxShadow: `inset ${COLOR_WIDTH}px 0 0 ${
                    settings.timelines[timeline]?.color ||
                    colors.structure.transparent
                  }`,
                }}
              >
                <MetadataPrettyPredictionsContent
                  name={timeline}
                  predictions={predictions}
                />
              </div>
            )
          })}
      </div>
    </>
  )
}

MetadataCuesContent.propTypes = {
  metadata: PropTypes.objectOf(
    PropTypes.arrayOf(
      PropTypes.shape({
        content: PropTypes.string.isRequired,
        score: PropTypes.number.isRequired,
      }).isRequired,
    ).isRequired,
  ).isRequired,
}

export default MetadataCuesContent
