import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from '../Filter/shape'

import { colors, constants, spacing, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'
import FilterReset from '../Filter/Reset'
import FilterSearch from '../Filter/Search'

import { getNewFacets, dispatch, ACTIONS, encode } from '../Filters/helpers'

export const noop = () => {}

const FilterFacet = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    type,
    attribute,
    values: { facets = [] },
    isDisabled,
  },
  filterIndex,
}) => {
  const [searchString, setSearchString] = useState('')

  const encodedFilter = encode({ filters: { type, attribute } })

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

      <FilterSearch
        placeholder="Filter facets"
        searchString={searchString}
        onChange={({ value }) => {
          setSearchString(value)
        }}
      />

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
          if (!key.toLowerCase().includes(searchString)) return null

          const offset = Math.ceil((docCount * 100) / largestCount)
          const facetIndex = facets.findIndex((f) => f === key)
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

                  const newFacets = getNewFacets({
                    facets,
                    isSelected,
                    hasModifier,
                    facetIndex,
                    key,
                  })

                  const values =
                    newFacets.length > 0 ? { facets: newFacets } : {}

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

FilterFacet.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterFacet
