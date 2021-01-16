import PropTypes from 'prop-types'
import { useEffect } from 'react'
import AutoSizer from 'react-virtualized-auto-sizer'
import useSWR from 'swr'

import { getQueryString } from '../Fetch/helpers'

import { colors, constants, spacing, typography } from '../Styles'

import settingsShape from './settingsShape'

import ModelMatrixTable from './Table'
import ModelMatrixLabels from './Labels'

const ModelMatrixMatrix = ({
  projectId,
  modelId,
  settings,
  dispatch,
  setMatrixDetails,
}) => {
  const queryString = getQueryString({
    minScore: settings.minScore,
    maxScore: settings.maxScore,
  })

  const {
    data: matrix,
    data: { name, overallAccuracy, labels, moduleName },
  } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/confusion_matrix/${queryString}`,
  )

  useEffect(() => {
    setMatrixDetails({ name, overallAccuracy, labels, moduleName })
  }, [setMatrixDetails, name, overallAccuracy, labels, moduleName])

  return (
    <>
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
          {/* begin placeholder for row labels */}
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
          {/* end placeholder for row labels */}

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
    </>
  )
}

ModelMatrixMatrix.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  settings: PropTypes.shape(settingsShape).isRequired,
  dispatch: PropTypes.func.isRequired,
  setMatrixDetails: PropTypes.func.isRequired,
}

export default ModelMatrixMatrix
