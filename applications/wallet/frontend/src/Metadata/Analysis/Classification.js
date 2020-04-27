import PropTypes from 'prop-types'

import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing } from '../../Styles'

const BBOX_SIZE = 56

const MODULES = {
  'zvi-object-detection': {
    columns: ['bbox', 'label', 'score'],
    labels: { bbox: 'bbox', label: 'label', score: 'confidence score' },
  },
  'zvi-label-detection': {
    columns: ['label', 'score'],
    labels: { label: 'label', score: 'confidence score' },
  },
}

const MetadataAnalysisClassification = ({ moduleName, moduleIndex }) => {
  const attr = `analysis.${moduleName}&width=${BBOX_SIZE}`

  const {
    query: { projectId, id: assetId },
  } = useRouter()

  const {
    data: {
      [moduleName]: { predictions },
    },
  } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/box_images/?attr=${attr}`,
  )

  const predictionColumns = Object.keys(predictions[0])

  // filter from MODULES which holds the module column names in the correct order
  const columns = MODULES[moduleName].columns.filter((column) => {
    return predictionColumns.includes(column)
  })

  return (
    <div
      css={{
        borderTop: moduleIndex !== 0 ? constants.borders.largeDivider : '',
        padding: spacing.normal,
      }}
    >
      <div
        css={{
          fontFamily: 'Roboto Mono',
          color: colors.structure.white,
          paddingBottom: spacing.normal,
        }}
      >
        {moduleName}
      </div>
      <table
        css={{
          fontFamily: 'Roboto Mono',
          color: colors.structure.white,
          width: '100%',
          td: {
            paddingRight: spacing.base,
          },
        }}
      >
        <thead>
          <tr>
            {columns.map((column, index) => {
              const shouldExpand = index === columns.length - 2

              return (
                <td
                  key={column}
                  css={{
                    fontFamily: 'Roboto Condensed',
                    textTransform: 'uppercase',
                    color: colors.structure.steel,
                    width: shouldExpand ? '100%' : '',
                    paddingBottom: spacing.normal,
                    '&:last-child': {
                      textAlign: 'right',
                      whiteSpace: 'nowrap',
                      paddingRight: 0,
                    },
                  }}
                >
                  {MODULES[moduleName].labels[column]}
                </td>
              )
            })}
          </tr>
        </thead>
        <tbody>
          {predictions.map((prediction, index) => {
            const isLastRow = index === predictions.length - 1

            return (
              <tr key={prediction.score}>
                {columns.map((column) => {
                  if (column === 'bbox') {
                    return (
                      <td
                        key={column}
                        css={{
                          display: 'flex',
                          paddingBottom: isLastRow ? 0 : spacing.normal,
                        }}
                      >
                        <img
                          css={{
                            height: BBOX_SIZE,
                            width: BBOX_SIZE,
                            objectFit: 'contain',
                          }}
                          alt={prediction.bbox}
                          title={prediction.bbox}
                          src={prediction.b64_image}
                        />
                      </td>
                    )
                  }

                  return (
                    <td
                      key={column}
                      css={{
                        '&:last-child': {
                          textAlign: 'right',
                          paddingRight: 0,
                        },
                        paddingRight: spacing.base,
                        paddingBottom: isLastRow ? 0 : spacing.normal,
                      }}
                    >
                      {prediction[column]}
                    </td>
                  )
                })}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

MetadataAnalysisClassification.propTypes = {
  moduleName: PropTypes.string.isRequired,
  moduleIndex: PropTypes.number.isRequired,
}

export default MetadataAnalysisClassification
