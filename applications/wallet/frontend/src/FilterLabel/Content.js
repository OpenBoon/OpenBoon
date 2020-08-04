import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from '../Filter/shape'

import { colors, constants, spacing, typography } from '../Styles'

import Select from '../Select'
import Button, { VARIANTS } from '../Button'
import FilterReset from '../Filter/Reset'

import { getNewFacets, dispatch, ACTIONS, encode } from '../Filters/helpers'

export const noop = () => {}

const FilterLabel = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    type,
    attribute,
    modelId,
    values: { labels = [], scope = 'all' },
    isDisabled,
  },
  filterIndex,
}) => {
  const encodedFilter = encode({ filters: { type, modelId } })

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )
  const { results } = data || {}

  const { buckets = [] } = results || {}

  const { docCount: largestCount = 1 } = buckets.find(({ key }) => !!key) || {}

  return (
    <>
      <FilterReset
        pathname={pathname}
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        onReset={noop}
      />

      <div css={{ height: spacing.moderate }} />

      <Select
        label="Scope"
        options={[
          { value: 'all', label: 'All' },
          { value: 'test', label: 'Test' },
          { value: 'train', label: 'Train' },
        ]}
        defaultValue={scope}
        onChange={({ value }) => {
          dispatch({
            type: ACTIONS.UPDATE_FILTER,
            payload: {
              pathname,
              projectId,
              assetId,
              filters,
              updatedFilter: {
                ...filter,
                values: { ...filter.values, scope: value },
              },
              filterIndex,
            },
          })
        }}
        isRequired={false}
        style={{
          width: '100%',
          height: 'auto',
          paddingTop: spacing.base,
          paddingBottom: spacing.base,
          backgroundColor: colors.structure.smoke,
        }}
      />

      <div css={{ height: spacing.moderate }} />

      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          paddingBottom: spacing.base,
          fontFamily: typography.family.condensed,
          color: colors.structure.zinc,
        }}
      >
        <div>LABEL</div>
        <div>COUNT</div>
      </div>

      <ul css={{ margin: 0, padding: 0, listStyle: 'none' }}>
        {buckets.map(({ key, docCount = 0 }) => {
          const offset = Math.ceil((docCount * 100) / largestCount)
          const facetIndex = labels.findIndex((f) => f === key)
          const isSelected = !!(facetIndex + 1)

          return (
            <li key={key}>
              <Button
                aria-label={key}
                style={{
                  width: '100%',
                  flexDirection: 'row',
                  backgroundColor: isSelected
                    ? `${colors.signal.sky.base}${constants.opacity.hex22Pct}`
                    : '',
                  color: colors.structure.zinc,
                  ':hover': {
                    backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
                    color: colors.structure.white,
                  },
                }}
                variant={VARIANTS.NEUTRAL}
                onClick={(event) => {
                  const hasModifier = event.metaKey || event.ctrlKey

                  const newLabels = getNewFacets({
                    facets: labels,
                    isSelected,
                    hasModifier,
                    facetIndex,
                    key,
                  })

                  const values =
                    newLabels.length > 0 ? { labels: newLabels } : {}

                  dispatch({
                    type: ACTIONS.UPDATE_FILTER,
                    payload: {
                      pathname,
                      projectId,
                      assetId,
                      filters,
                      updatedFilter: {
                        type,
                        attribute,
                        modelId,
                        values,
                      },
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

FilterLabel.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterLabel
