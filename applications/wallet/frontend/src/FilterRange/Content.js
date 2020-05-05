import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import { bytesToSize } from '../Bytes/helpers'

import { colors, constants, spacing } from '../Styles'

import { dispatch, ACTIONS, encode } from '../Filters/helpers'
import FiltersReset from '../Filters/Reset'

import FilterRangeSlider from './Slider'

const MIN_WIDTH = 76

const formatValue = ({ attribute, value }) => {
  if (attribute.includes('size')) {
    return bytesToSize({ bytes: value })
  }

  return Math.ceil(value)
}

const FilterRange = ({
  projectId,
  assetId,
  filters,
  filter: { type, attribute, values },
  filterIndex,
}) => {
  const {
    data: { results },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encode({
      filters: { type, attribute, values },
    })}`,
  )

  const domain = [results.min, results.max]
  const cachedRange = values.min ? [values.min, values.max] : domain

  const [rangeValues, setRangeValues] = useState(cachedRange)

  return (
    <div>
      <FiltersReset
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        updatedFilter={{ type, attribute }}
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
            onChange={(value) =>
              dispatch({
                action: ACTIONS.UPDATE_FILTER,
                payload: {
                  projectId,
                  assetId,
                  filters,
                  updatedFilter: {
                    type,
                    attribute,
                    values: { min: value[0], max: value[1] },
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
  filters: PropTypes.arrayOf(
    PropTypes.shape({
      type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
      attribute: PropTypes.string,
      values: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    }).isRequired,
  ).isRequired,
  filter: PropTypes.shape({
    type: PropTypes.oneOf(['range']).isRequired,
    attribute: PropTypes.string.isRequired,
    values: PropTypes.shape({ min: PropTypes.number, max: PropTypes.number }),
  }).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterRange
