import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import ButtonCopy, { COPY_SIZE } from '../Button/Copy'
import Pills from '../Pills'

export const BBOX_SIZE = 56

const COLUMNS = ['bbox', 'label', 'score']

const MetadataAnalysisLabelDetection = ({ name, value: { predictions } }) => {
  const predictionColumns = Object.keys(predictions[0])

  // filter from COLUMNS which holds the module column names in the correct order
  const columns = COLUMNS.filter((column) => {
    return predictionColumns.includes(column)
  })

  const tags = predictions
    .flatMap((prediction) => prediction.tags)
    .filter((tag) => !!tag)

  return (
    <>
      <div
        css={{
          padding: spacing.normal,
          paddingBottom: spacing.comfy,
          '&:not(:first-of-type)': {
            borderTop: constants.borders.largeDivider,
          },
        }}
      >
        <div
          css={{
            fontFamily: 'Roboto Mono',
            fontSize: typography.size.small,
            lineHeight: typography.height.small,
            color: colors.structure.white,
            paddingBottom: spacing.normal,
          }}
        >
          {name}
        </div>
        <table
          css={{
            fontFamily: 'Roboto Mono',
            fontSize: typography.size.small,
            lineHeight: typography.height.small,
            color: colors.structure.white,
            width: '100%',
            borderSpacing: 0,
            td: {
              paddingRight: spacing.base,
              paddingLeft: 0,
            },
          }}
        >
          <thead>
            <tr>
              {columns.map((column) => {
                return (
                  <th
                    key={column}
                    css={{
                      fontFamily: 'Roboto Condensed',
                      fontWeight: typography.weight.regular,
                      textTransform: 'uppercase',
                      color: colors.structure.steel,
                      paddingBottom: spacing.normal,
                      paddingLeft: 0,
                      textAlign: 'left',
                      '&:last-of-type': {
                        textAlign: 'right',
                        whiteSpace: 'nowrap',
                        paddingRight: 0,
                      },
                      '&:nth-last-of-type(2)': { width: '100%' },
                    }}
                  >
                    {column === 'score' ? 'confidence score' : column}
                  </th>
                )
              })}
            </tr>
          </thead>
          <tbody>
            {predictions.map((prediction, index) => {
              return (
                <tr
                  // eslint-disable-next-line react/no-array-index-key
                  key={`${prediction.label}-${index}`}
                  css={{
                    verticalAlign: 'bottom',
                    td: {
                      paddingTop: spacing.base,
                      paddingBottom: spacing.base,
                      paddingRight: spacing.base,
                      borderBottom: constants.borders.divider,
                    },
                    '&:first-of-type': { td: { paddingTop: 0 } },
                  }}
                >
                  {columns.map((column) => {
                    if (column === 'bbox') {
                      return (
                        <td key={column} css={{ display: 'flex' }}>
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
      {tags.length > 0 && (
        <div
          css={{
            padding: spacing.normal,
            paddingTop: 0,
            ':hover': {
              backgroundColor: colors.signal.electricBlue.background,
              div: {
                svg: {
                  display: 'inline-block',
                },
              },
            },
          }}
        >
          <div css={{ display: 'flex' }}>
            <div
              css={{
                minHeight: COPY_SIZE,
                width: '100%',
                fontFamily: 'Roboto Condensed',
                textTransform: 'uppercase',
                color: colors.structure.steel,
                borderTop: constants.borders.divider,
                paddingTop: spacing.normal,
                paddingBottom: spacing.base,
              }}
            >
              tags
            </div>
            <ButtonCopy value={JSON.stringify(tags)} />
          </div>
          <div>
            <Pills>{tags}</Pills>
          </div>
        </div>
      )}
    </>
  )
}

MetadataAnalysisLabelDetection.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    predictions: PropTypes.arrayOf(PropTypes.shape({})).isRequired,
  }).isRequired,
}

export default MetadataAnalysisLabelDetection
