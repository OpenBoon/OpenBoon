import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'
import FiltersReset from '../Filters/Reset'

import filterShape from '../Filter/shape'

import { dispatch, ACTIONS, encode } from '../Filters/helpers'

import FilterRangeSlider from '../FilterRange/Slider'

import { formatRange } from './helpers'

const FilterLabelConfidence = ({
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

  const [rangeValues, setRangeValues] = useState([min, max])

  const { docCount: largestCount = 1 } = buckets.find(({ key }) => !!key) || {}

  const hasSelections = labels.length > 0

  return (
    <>
      <FiltersReset
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        onReset={() => setRangeValues([0, 1])}
      />
      <div css={{ paddingBottom: spacing.moderate }}>
        Label prediction confidence score:{' '}
        {formatRange({ min: rangeValues[0], max: rangeValues[1] })}
      </div>
      <div css={{ padding: spacing.normal, paddingBottom: spacing.spacious }}>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            paddingBottom: spacing.moderate,
            fontFamily: 'Roboto Mono',
          }}
        >
          <span>0.00</span>
          <span>1.00</span>
        </div>
        <div css={{ padding: spacing.small }}>
          <FilterRangeSlider
            step={0.01}
            domain={[0, 1]}
            values={rangeValues}
            setValues={(values) => setRangeValues(values)}
            onChange={([newMin, newMax]) =>
              dispatch({
                action: ACTIONS.UPDATE_FILTER,
                payload: {
                  projectId,
                  assetId,
                  filters,
                  updatedFilter: {
                    type,
                    attribute,
                    values: { labels, min: newMin, max: newMax },
                  },
                  filterIndex,
                },
              })
            }
          />
        </div>
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

FilterLabelConfidence.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterLabelConfidence
