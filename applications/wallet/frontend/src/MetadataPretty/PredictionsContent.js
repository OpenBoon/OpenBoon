import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import { colors, constants, spacing, typography } from '../Styles'

import ButtonCopy, { COPY_SIZE } from '../Button/Copy'
import Pills from '../Pills'
import Button, { VARIANTS } from '../Button'

import {
  ACTIONS as FILTER_ACTIONS,
  dispatch as filterDispatch,
} from '../Filters/helpers'

export const BBOX_SIZE = 56

const COLUMNS = ['bbox', 'label', 'content', 'score']

export const FILTER_TYPES = { labels: 'labelConfidence', text: 'textContent' }

const MetadataPrettyPredictionsContent = ({
  path,
  name,
  type,
  predictions,
}) => {
  const {
    pathname,
    query: { projectId, assetId, query },
  } = useRouter()

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
            borderTop: constants.borders.large.smoke,
          },
        }}
      >
        <div
          css={{
            fontFamily: typography.family.mono,
            fontSize: typography.size.small,
            lineHeight: typography.height.small,
            color: colors.structure.white,
            paddingBottom: spacing.normal,
          }}
        >
          {(path && type && (
            <Button
              aria-label="Add Filter"
              variant={VARIANTS.NEUTRAL}
              onClick={() => {
                filterDispatch({
                  type: FILTER_ACTIONS.ADD_VALUE,
                  payload: {
                    pathname,
                    projectId,
                    assetId,
                    filter: {
                      type: FILTER_TYPES[type],
                      attribute: `${path}.${name}`,
                      values: {},
                    },
                    query,
                  },
                })
              }}
            >
              {name}
            </Button>
          )) ||
            name}
        </div>

        <table>
          <thead>
            <tr>
              {columns.map((column) => {
                return (
                  <th
                    key={column}
                    css={{
                      fontFamily: typography.family.condensed,
                      fontWeight: typography.weight.regular,
                      textTransform: 'uppercase',
                      color: colors.structure.steel,
                      padding: 0,
                      paddingBottom: spacing.base,
                      borderBottom: constants.borders.regular.smoke,
                      textAlign: 'left',
                      '&:last-of-type': {
                        textAlign: 'right',
                        whiteSpace: 'nowrap',
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
            {predictions.map((prediction) => {
              return (
                <tr
                  key={`${prediction.label || prediction.content}-${
                    prediction.score
                  }`}
                  css={{
                    td: {
                      verticalAlign: 'bottom',
                      fontFamily: typography.family.mono,
                      fontSize: typography.size.small,
                      lineHeight: typography.height.small,
                      padding: spacing.base,
                      paddingLeft: 0,
                      paddingRight: spacing.base,
                      borderBottom: constants.borders.regular.smoke,
                    },
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
                          &nbsp;
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
              backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
              svg: { opacity: 1 },
            },
          }}
        >
          <div css={{ display: 'flex' }}>
            <div
              css={{
                minHeight: COPY_SIZE,
                width: '100%',
                fontFamily: typography.family.condensed,
                textTransform: 'uppercase',
                color: colors.structure.steel,
                borderTop: constants.borders.regular.smoke,
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

MetadataPrettyPredictionsContent.defaultProps = {
  path: undefined,
  type: undefined,
}

MetadataPrettyPredictionsContent.propTypes = {
  path: PropTypes.string,
  name: PropTypes.string.isRequired,
  type: PropTypes.oneOf(['labels', 'text']),
  predictions: PropTypes.arrayOf(
    PropTypes.shape({
      bbox: PropTypes.arrayOf(PropTypes.number),
      label: PropTypes.string,
      content: PropTypes.string,
      score: PropTypes.number.isRequired,
    }).isRequired,
  ).isRequired,
}

export default MetadataPrettyPredictionsContent
