import PropTypes from 'prop-types'
import AutoSizer from 'react-virtualized-auto-sizer'

import { colors, constants, spacing, typography } from '../Styles'

import ModelMatrixTable from './Table'
import ModelMatrixLabels from './Labels'

const ModelMatrixMatrix = ({ matrix, settings, dispatch }) => {
  return (
    <div
      css={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        width: '0%',
      }}
    >
      <div
        css={{
          flex: 1,
          display: 'flex',
          width: '100%',
        }}
      >
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            padding: spacing.normal,
            borderRight: constants.borders.regular.coal,
          }}
        >
          <div
            css={{
              writingMode: 'vertical-lr',
              transform: 'rotate(180deg)',
            }}
          >
            <span
              css={{
                fontWeight: typography.weight.bold,
                fontSize: typography.size.medium,
                lineHeight: typography.height.medium,
              }}
            >
              True Label
            </span>{' '}
            <span css={{ color: colors.structure.zinc }}>(Number in Set)</span>
          </div>
        </div>

        <div css={{ width: '100%' }}>
          <AutoSizer defaultWidth={800} defaultHeight={600}>
            {({ width, height }) => (
              <ModelMatrixTable
                matrix={matrix}
                width={width}
                height={height}
                settings={settings}
                dispatch={dispatch}
              />
            )}
          </AutoSizer>
        </div>
      </div>

      <div
        css={{
          display: 'flex',
          borderTop: constants.borders.regular.coal,
          width: '100%',
        }}
      >
        {/* begin placeholder for "True Label" column width */}
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            padding: spacing.normal,
            borderRight: constants.borders.regular.transparent,
          }}
        >
          <div
            css={{
              writingMode: 'vertical-lr',
              transform: 'rotate(180deg)',
              lineHeight: typography.height.medium,
            }}
          >
            &nbsp;
          </div>
        </div>
        {/* end placeholder for "True Label" column width */}

        <div css={{ display: 'flex', width: settings.width }}>
          {/* begin placeholde for row labels */}
          <div
            css={{
              paddingLeft: spacing.normal,
              width: settings.labelsWidth,
              minWidth: settings.labelsWidth,
              borderRight: constants.borders.regular.coal,
            }}
          >
            &nbsp;
          </div>
          {/* end placeholde for row labels */}

          <div css={{ flex: 1, width: '0%' }}>
            <div
              css={{
                display: 'flex',
                flexDirection: 'column',
              }}
            >
              <ModelMatrixLabels matrix={matrix} settings={settings} />

              <div
                css={{
                  padding: spacing.normal,
                  textAlign: 'center',
                  fontWeight: typography.weight.bold,
                  fontSize: typography.size.medium,
                  lineHeight: typography.height.medium,
                }}
              >
                Predictions
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

ModelMatrixMatrix.propTypes = {
  matrix: PropTypes.shape({
    name: PropTypes.string.isRequired,
    overallAccuracy: PropTypes.number.isRequired,
    labels: PropTypes.arrayOf(PropTypes.string).isRequired,
    matrix: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)).isRequired,
  }).isRequired,
  settings: PropTypes.shape({
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    labelsWidth: PropTypes.number.isRequired,
    zoom: PropTypes.number.isRequired,
    isMinimapOpen: PropTypes.bool.isRequired,
    isNormalized: PropTypes.bool.isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelMatrixMatrix
