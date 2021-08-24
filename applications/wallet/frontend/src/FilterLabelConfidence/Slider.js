import { useState } from 'react'
import PropTypes from 'prop-types'

import { spacing, typography } from '../Styles'

import filterShape from '../Filter/shape'

import FilterReset from '../Filter/Reset'

import { dispatch, ACTIONS } from '../Filters/helpers'

import Slider from '../Slider'

import { formatRange } from './helpers'

const FilterLabelConfidenceSlider = ({
  pathname,
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
        pathname={pathname}
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        onReset={() => setRangeValues([0, 1])}
      />

      <div
        css={{ paddingTop: spacing.base, fontStyle: typography.style.italic }}
      >
        Confidence Score:{' '}
        {formatRange({ min: rangeValues[0], max: rangeValues[1], labels })}
      </div>

      <div css={{ padding: spacing.normal, paddingBottom: spacing.spacious }}>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            paddingBottom: spacing.normal,
            fontFamily: typography.family.mono,
          }}
        >
          <span>0.00</span>
          <span>1.00</span>
        </div>

        <div css={{ padding: spacing.small }}>
          <Slider
            mode="both"
            step={0.01}
            domain={[0, 1]}
            values={rangeValues}
            isMuted={!!isDisabled}
            isDisabled={labels.length === 0}
            onUpdate={(values) => setRangeValues(values)}
            onChange={([newMin, newMax]) =>
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
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterLabelConfidenceSlider
