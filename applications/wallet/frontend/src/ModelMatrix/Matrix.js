import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import AutoSizer from 'react-virtualized-auto-sizer'

import { colors, constants, spacing, typography } from '../Styles'
import { getQueryString } from '../Fetch/helpers'

import ModelMatrixTable from './Table'
import ModelMatrixLabels from './Labels'
import ModelMatrixPreview from './Preview'

const PANEL_WIDTH = 200

const ModelMatrixMatrix = ({ settings, dispatch }) => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const queryParams = getQueryString({
    minScore: settings.minScore,
    maxScore: settings.maxScore,
  })

  const { data: matrix } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/confusion_matrix/${queryParams}`,
  )

  return (
    <>
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
              <span css={{ color: colors.structure.zinc }}>
                (Number in Set)
              </span>
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

          <div css={{ display: 'flex', width: settings.width }}>
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

      {settings.isPreviewOpen && (
        <div
          css={{
            display: 'flex',
            flexDirection: 'column',
            width: PANEL_WIDTH,
            height: '100%',
            borderLeft: constants.borders.regular.coal,
            overflow: 'auto',
          }}
        >
          <ModelMatrixPreview
            matrix={matrix}
            selectedCell={settings.selectedCell}
          />
        </div>
      )}
    </>
  )
}

ModelMatrixMatrix.propTypes = {
  settings: PropTypes.shape({
    minScore: PropTypes.number.isRequired,
    maxScore: PropTypes.number.isRequired,
    isPreviewOpen: PropTypes.bool.isRequired,
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    labelsWidth: PropTypes.number.isRequired,
    zoom: PropTypes.number.isRequired,
    isMinimapOpen: PropTypes.bool.isRequired,
    isNormalized: PropTypes.bool.isRequired,
    selectedCell: PropTypes.arrayOf(PropTypes.number).isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelMatrixMatrix
