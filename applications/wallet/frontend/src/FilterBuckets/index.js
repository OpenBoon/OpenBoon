import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { colors, constants, spacing, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'

import {
  getNewLabels,
  getUpdatedFilter,
  dispatch,
  ACTIONS,
} from '../Filters/helpers'

const FilterBuckets = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter: {
    type,
    attribute,
    modelId,
    values: { labels: l, facets: f, scope = 'all', min = 0.0, max = 1.0 },
    isDisabled,
  },
  filterIndex,
  buckets,
  searchString,
}) => {
  const labels = l || f || []

  const { docCount: largestCount = 1 } = buckets.find(({ key }) => !!key) || {}

  return (
    <>
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          paddingBottom: spacing.normal,
        }}
      >
        <div css={{ fontStyle: typography.style.italic }}>
          {type === 'labelConfidence' ? 'Predictions' : 'Labels'} Selected:{' '}
          {labels.length}
        </div>
        <div>
          <Button
            variant={VARIANTS.MICRO}
            onClick={() => {
              const newLabels = buckets.reduce((acc, { key }) => {
                return [...acc, key]
              }, [])

              const updatedFilter = getUpdatedFilter({
                type,
                attribute,
                modelId,
                scope,
                min,
                max,
                newLabels,
              })

              dispatch({
                type: ACTIONS.UPDATE_FILTER,
                payload: {
                  pathname,
                  projectId,
                  assetId,
                  filters,
                  updatedFilter,
                  filterIndex,
                },
              })
            }}
          >
            Select All
          </Button>
        </div>
      </div>

      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          paddingBottom: spacing.base,
          fontFamily: typography.family.condensed,
          color: colors.structure.zinc,
        }}
      >
        <div>{type === 'labelConfidence' ? 'PREDICTION' : 'LABEL'}</div>
        <div>COUNT</div>
      </div>

      <ul css={{ margin: 0, padding: 0, listStyle: 'none' }}>
        {buckets.map(({ key, docCount = 0 }) => {
          if (!key.toLowerCase().includes(searchString.toLowerCase())) {
            return null
          }

          const offset = Math.ceil((docCount * 100) / largestCount)
          const labelIndex = labels.findIndex((label) => label === key)
          const isSelected = !!(labelIndex + 1)

          return (
            <li key={key}>
              <Button
                aria-label={key}
                style={{
                  width: '100%',
                  flexDirection: 'row',
                  backgroundColor: isSelected
                    ? `${colors.signal.sky.base}${constants.opacity.hex22Pct}`
                    : colors.structure.transparent,
                  color: isSelected
                    ? colors.structure.white
                    : colors.structure.zinc,
                  ':hover, &.focus-visible:focus': {
                    backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
                    color: colors.structure.white,
                  },
                }}
                variant={VARIANTS.NEUTRAL}
                onClick={(event) => {
                  const hasModifier = event.metaKey || event.ctrlKey

                  const newLabels = getNewLabels({
                    labels,
                    isSelected,
                    hasModifier,
                    labelIndex,
                    key,
                  })

                  const updatedFilter = getUpdatedFilter({
                    type,
                    attribute,
                    modelId,
                    scope,
                    min,
                    max,
                    newLabels,
                  })

                  dispatch({
                    type: ACTIONS.UPDATE_FILTER,
                    payload: {
                      pathname,
                      projectId,
                      assetId,
                      filters,
                      updatedFilter,
                      filterIndex,
                    },
                  })
                }}
              >
                <div css={{ width: '100%' }}>
                  <div css={{ display: 'flex' }}>
                    <div
                      css={{
                        width: `${offset}%`,
                        borderTop:
                          isDisabled || !isSelected
                            ? constants.borders.large.steel
                            : constants.borders.keyOneLarge,
                      }}
                    />
                    <div
                      css={{
                        height: 4,
                        width: `${100 - offset}%`,
                        borderTop: constants.borders.regular.smoke,
                      }}
                    />
                  </div>
                  <div
                    css={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      padding: spacing.base,
                      fontFamily: typography.family.mono,
                      fontSize: typography.size.small,
                      lineHeight: typography.height.small,
                    }}
                  >
                    <div
                      css={{
                        overflow: 'hidden',
                        whiteSpace: 'nowrap',
                        textOverflow: 'ellipsis',
                      }}
                    >
                      {key}
                    </div>
                    <div css={{ paddingLeft: spacing.base }}>{docCount}</div>
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

FilterBuckets.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
  buckets: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      docCount: PropTypes.number,
    }),
  ).isRequired,
  searchString: PropTypes.string.isRequired,
}

export default FilterBuckets
