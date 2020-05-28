import { useState } from 'react'
import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import filterShape from '../Filter/shape'

import FilterReset from '../Filter/Reset'

import { dispatch, ACTIONS } from '../Filters/helpers'

import FilterRangeSlider from '../FilterRange/Slider'

import { formatRange } from './helpers'

const FilterLabelConfidenceSlider = ({
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    type,
    attribute,
    values: { labels = [], min = 0.0, max = 1.0 },
    isDisabled,
  },
  filterIndex,
}) => {
  const [rangeValues, setRangeValues] = useState([min, max])

  return (
    <>
      <FilterReset
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        onReset={() => setRangeValues([0, 1])}
      />
      <div>
        Label prediction confidence score:{' '}
        {formatRange({ min: rangeValues[0], max: rangeValues[1] })}
      </div>
      <div css={{ padding: spacing.normal, paddingBottom: spacing.spacious }}>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            paddingBottom: spacing.normal,
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
            isDisabled={!!isDisabled}
            onUpdate={(values) => setRangeValues(values)}
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
    </>
  )
}

FilterLabelConfidenceSlider.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterLabelConfidenceSlider
