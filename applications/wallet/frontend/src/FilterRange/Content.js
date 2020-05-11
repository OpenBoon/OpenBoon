import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import { bytesToSize } from '../Bytes/helpers'

import filterShape from '../Filter/shape'

import { colors, constants, spacing } from '../Styles'

import { dispatch, ACTIONS, encode } from '../Filters/helpers'
import FiltersReset from '../Filters/Reset'

import FilterRangeSlider from './Slider'

const MIN_WIDTH = 76

const formatValue = ({ attribute, value }) => {
  if (attribute.includes('size')) {
    return bytesToSize({ bytes: value })
  }

  // Will always return 2 decimals at most, only if necessary
  return Math.round((value + Number.EPSILON) * 100) / 100
}

const FilterRange = ({
  projectId,
  assetId,
  filters,
  filter,
  filter: { type, attribute, values },
  filterIndex,
}) => {
  const {
    data: { results },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encode({
      filters: { type, attribute },
    })}`,
  )

  const domain = [results.min, results.max]

  const [rangeValues, setRangeValues] = useState([
    values.min || results.min,
    values.max || results.max,
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
          }}
        >
          <span>{formatValue({ attribute, value: results.min })}</span>
          <span>{formatValue({ attribute, value: results.max })}</span>
        </div>
        <div css={{ padding: spacing.small }}>
          <FilterRangeSlider
            domain={domain}
            values={rangeValues}
            setValues={(value) => setRangeValues(value)}
            onChange={([min, max]) =>
              dispatch({
                action: ACTIONS.UPDATE_FILTER,
                payload: {
                  projectId,
                  assetId,
                  filters,
                  updatedFilter: {
                    type,
                    attribute,
                    values: { min, max },
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
