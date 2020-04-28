import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

const COLUMNS = ['label', 'score']

const MetadataLabelDetection = ({ name, predictions }) => {
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
        {name}
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
                    '&:last-child': {
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
          {predictions.map((prediction, index) => {
            const isFirstRow = index === 0
            const isLastRow = index === predictions.length - 1

            return (
              <tr key={prediction.score} css={{ verticalAlign: 'bottom' }}>
                {columns.map((column) => {
                  return (
                    <td
                      key={column}
                      css={{
                        '&:last-child': {
                          textAlign: 'right',
                          paddingRight: 0,
                        },
                        paddingRight: spacing.base,
                        paddingTop: isFirstRow ? 0 : spacing.base,
                        paddingBottom: isLastRow ? 0 : spacing.base,
                        borderBottom: isLastRow
                          ? ''
                          : constants.borders.divider,
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

MetadataLabelDetection.propTypes = {
  name: PropTypes.string.isRequired,
  predictions: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
}

export default MetadataLabelDetection
