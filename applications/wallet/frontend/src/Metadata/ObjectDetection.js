import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

const MODULE_NAME = 'zvi-object-detection'
const COLUMNS = ['bbox', 'label', 'score']
const BBOX_SIZE = 56

const MetadataObjectDetection = () => {
  const attr = `analysis.${MODULE_NAME}&width=${BBOX_SIZE}`

  const {
    query: { projectId, id: assetId },
  } = useRouter()

  const {
    data: {
      [MODULE_NAME]: { predictions },
    },
  } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/box_images/?attr=${attr}`,
  )

  const predictionColumns = Object.keys(predictions[0])

  // filter from COLUMNS which holds the module column names in the correct order
  const columns = COLUMNS.filter((column) => {
    return predictionColumns.includes(column)
  })

  return (
    <div
      css={{
        '&:not(:first-of-type)': { borderTop: constants.borders.largeDivider },
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
        {MODULE_NAME}
      </div>
      <table
        css={{
          fontFamily: 'Roboto Mono',
          color: colors.structure.white,
          width: '100%',
          borderSpacing: 0,
          td: {
            paddingRight: spacing.base,
          },
        }}
      >
        <thead>
          <tr>
            {columns.map((column, index) => {
              // make second to last column expand to push last column to the end
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
                    '&:last-of-type': {
                      textAlign: 'right',
                      whiteSpace: 'nowrap',
                      paddingRight: 0,
                    },
                  }}
                >
                  {column === 'score' ? 'confidence score' : column}
                </td>
              )
            })}
          </tr>
        </thead>
        <tbody>
          {predictions.map((prediction) => {
            return (
              <tr
                key={prediction.score}
                css={{
                  verticalAlign: 'bottom',
                  td: {
                    paddingTop: spacing.base,
                    paddingBottom: spacing.base,
                    paddingRight: spacing.base,
                  },
                  '&:first-of-type': { td: { paddingTop: 0 } },
                  '&:last-of-type': {
                    td: {
                      paddingBottom: 0,
                    },
                  },
                  '&:not(:first-of-type)': {
                    td: {
                      borderTop: constants.borders.divider,
                    },
                  },
                }}
              >
                {columns.map((column) => {
                  if (column === 'bbox') {
                    return (
                      <td
                        key={column}
                        css={{
                          display: 'flex',
                        }}
                      >
                        <img
                          css={{
                            maxHeight: BBOX_SIZE,
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
                        '&:last-of-type': {
                          textAlign: 'right',
                          paddingRight: 0,
                        },
                        paddingRight: spacing.base,
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

export default MetadataObjectDetection
