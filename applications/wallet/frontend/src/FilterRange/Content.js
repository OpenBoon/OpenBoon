import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from '../Filter/shape'

import { colors, constants, spacing } from '../Styles'

import { dispatch, ACTIONS, encode } from '../Filters/helpers'
import FiltersReset from '../Filters/Reset'

import { formatValue } from './helpers'

import FilterRangeSlider from './Slider'

const MIN_WIDTH = 76

const FilterRange = ({
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    type,
    attribute,
    values: { min, max },
  },
  filterIndex,
}) => {
  const {
    data: { results },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encode({
      filters: { type, attribute },
    })}`,
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )

  const domain = [results.min, results.max]

  const [rangeValues, setRangeValues] = useState([
    min || results.min,
    max || results.max,
  ])

  return (
    <div>
      <FiltersReset
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        onReset={() => setRangeValues(domain)}
      />
      <div css={{ padding: spacing.normal }}>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            paddingBottom: spacing.moderate,
            fontFamily: 'Roboto Mono',
          }}
        >
          <span>{formatValue({ attribute, value: results.min })}</span>
          <span>{formatValue({ attribute, value: results.max })}</span>
        </div>
        <div css={{ padding: spacing.small }}>
          <FilterRangeSlider
            step={0.1}
            domain={domain}
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
                    values: { min: newMin, max: newMax },
                  },
                  filterIndex,
                },
              })
            }
          />
        </div>
        <div
          css={{
            paddingTop: spacing.comfy,
            color: colors.structure.zinc,
            display: 'flex',
            justifyContent: 'space-around',
          }}
        >
          <div css={{ display: 'flex', alignItems: 'center' }}>
            MIN &nbsp;
            <div
              css={{
                minWidth: MIN_WIDTH,
                backgroundColor: colors.structure.lead,
                paddingLeft: spacing.moderate,
                paddingRight: spacing.moderate,
                paddingTop: spacing.normal,
                paddingBottom: spacing.normal,
                textAlign: 'center',
                borderRadius: constants.borderRadius.small,
              }}
            >
              {formatValue({ attribute, value: rangeValues[0] })}
            </div>
          </div>
          <div css={{ display: 'flex', alignItems: 'center' }}>
            MAX &nbsp;
            <div
              css={{
                minWidth: MIN_WIDTH,
                backgroundColor: colors.structure.lead,
                paddingLeft: spacing.moderate,
                paddingRight: spacing.moderate,
                paddingTop: spacing.normal,
                paddingBottom: spacing.normal,
                textAlign: 'center',
                borderRadius: constants.borderRadius.small,
              }}
            >
              {formatValue({ attribute, value: rangeValues[1] })}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

FilterRange.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterRange
