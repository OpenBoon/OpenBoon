import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'
import { dispatch, ACTIONS } from '../Filters/helpers'

const FilterFacet = ({
  projectId,
  assetId,
  filters,
  filter: { type, attribute, values },
  filterIndex,
}) => {
  const encodedFilter = btoa(JSON.stringify({ type, attribute }))
  const {
    data: {
      results: { buckets },
    },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
  )

  const { docCount: largestCount = 1 } = buckets.find(({ key }) => !!key)

  const hasSelections = Object.keys(values).find((facet) => values[facet])

  return (
    <>
      <div
        css={{
          display: 'flex',
          paddingTop: spacing.base,
          paddingBottom: spacing.base,
        }}
      >
        <div css={{ flex: 1 }} />
        <Button
          style={{
            width: '100%',
            color: colors.structure.zinc,
            fontFamily: 'Roboto Condensed',
            ':hover': {
              color: colors.structure.white,
            },
          }}
          variant={VARIANTS.NEUTRAL}
          onClick={() =>
            dispatch({
              action: ACTIONS.UPDATE_FILTER,
              payload: {
                projectId,
                assetId,
                filters,
                updatedFilter: {
                  type,
                  attribute,
                  values: {},
                },
                filterIndex,
              },
            })
          }
        >
          RESET
        </Button>
      </div>
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          paddingTop: spacing.base,
          paddingBottom: spacing.base,
          fontFamily: 'Roboto Condensed',
          color: colors.structure.zinc,
        }}
      >
        <div>KEYWORD</div>
        <div>COUNT</div>
      </div>
      <ul css={{ margin: 0, padding: 0, listStyle: 'none' }}>
        {buckets.map(({ key, docCount = 0 }) => {
          const offset = Math.ceil((docCount * 100) / largestCount)
          const isSelected = values[key]

          return (
            <li key={key}>
              <Button
                style={{
                  width: '100%',
                  flexDirection: 'row',
                  backgroundColor: isSelected
                    ? colors.signal.electricBlue.background
                    : '',
                  color: hasSelections
                    ? colors.structure.zinc
                    : colors.structure.white,
                  ':hover': {
                    backgroundColor: colors.signal.electricBlue.background,
                    color: colors.structure.white,
                  },
                }}
                variant={VARIANTS.NEUTRAL}
                onClick={() =>
                  dispatch({
                    action: ACTIONS.UPDATE_FILTER,
                    payload: {
                      projectId,
                      assetId,
                      filters,
                      updatedFilter: {
                        type,
                        attribute,
                        values: { ...values, [key]: !values[key] },
                      },
                      filterIndex,
                    },
                  })
                }
              >
                <div css={{ width: '100%' }}>
                  <div css={{ display: 'flex' }}>
                    <div
                      css={{
                        width: `${offset}%`,
                        borderTop:
                          !isSelected && hasSelections
                            ? constants.borders.unselectedFacet
                            : constants.borders.facet,
                      }}
                    />
                    <div
                      css={{
                        height: 4,
                        width: `${100 - offset}%`,
                        borderTop: constants.borders.divider,
                      }}
                    />
                  </div>
                  <div
                    css={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      padding: spacing.base,
                    }}
                  >
                    <div>{key}</div>
                    <div>{docCount}</div>
                  </div>
                </div>
              </Button>
            </li>
          )
        })}
      </ul>
    </>
  )
}

FilterFacet.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(
    PropTypes.shape({
      type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
      attribute: PropTypes.string,
      values: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    }).isRequired,
  ).isRequired,
  filter: PropTypes.shape({
    type: PropTypes.oneOf(['facet']).isRequired,
    attribute: PropTypes.string.isRequired,
    values: PropTypes.shape({ exists: PropTypes.bool }),
  }).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterFacet
