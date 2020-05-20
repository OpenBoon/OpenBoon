import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'

import filterShape from '../Filter/shape'

import { dispatch, ACTIONS, encode } from '../Filters/helpers'
import FiltersSearch from '../Filters/Search'

import FilterLabelConfidenceSlider from './Slider'

const FilterLabelConfidenceContent = ({
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    type,
    attribute,
    values: { labels = [], min = 0.0, max = 1.0 },
  },
  filterIndex,
}) => {
  const [searchString, setSearchString] = useState('')

  const encodedFilter = encode({ filters: { type, attribute } })

  const {
    data: {
      results: { buckets },
    },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )

  const { docCount: largestCount = 1 } = buckets.find(({ key }) => !!key) || {}

  const hasSelections = labels.length > 0

  return (
    <>
      <FilterLabelConfidenceSlider
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
      />
      <FiltersSearch
        placeholder="Search labels"
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
          fontFamily: 'Roboto Condensed',
          color: colors.structure.zinc,
        }}
      >
        <div>KEYWORD</div>
        <div>COUNT</div>
      </div>
      <ul css={{ margin: 0, padding: 0, listStyle: 'none' }}>
        {buckets.map(({ key, docCount = 0 }) => {
          if (!key.toLowerCase().includes(searchString)) return null

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
                onClick={() => {
                  const newLabelConfidences = isSelected
                    ? [
                        ...labels.slice(0, facetIndex),
                        ...labels.slice(facetIndex + 1),
                      ]
                    : [...labels, key]

                  const values =
                    newLabelConfidences.length > 0
                      ? { labels: newLabelConfidences, min, max }
                      : {}

                  dispatch({
                    action: ACTIONS.UPDATE_FILTER,
                    payload: {
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
                      fontFamily: 'Roboto Mono',
                      fontSize: typography.size.small,
                      lineHeight: typography.height.small,
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

FilterLabelConfidenceContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterLabelConfidenceContent
