import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { spacing } from '../Styles'

import FilterRangeSlider from './Slider'

const FilterRange = ({
  projectId,
  //   assetId,
  //   filters,
  //   filter,
  filter: { type, attribute },
  //   filterIndex,
}) => {
  const encodedFilter = btoa(JSON.stringify({ type, attribute }))

  const {
    data: { results },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
  )

  const domain = [results.min, results.max]

  const [values, setValues] = useState(domain)

  return (
    <div>
      <div css={{ padding: spacing.spacious }}>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            paddingBottom: spacing.moderate,
          }}
        >
          <span>{results.min}</span>
          <span>{results.max}</span>
        </div>
        <div css={{ padding: spacing.small }}>
          <FilterRangeSlider
            domain={domain}
            values={values}
            setValues={setValues}
            onChange={() => {}}
          />
        </div>
      </div>
    </div>
  )
}

FilterRange.propTypes = {
  projectId: PropTypes.string.isRequired,
  //   assetId: PropTypes.string.isRequired,
  //   filters: PropTypes.arrayOf(
  //     PropTypes.shape({
  //       type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
  //       attribute: PropTypes.string,
  //       values: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  //     }).isRequired,
  //   ).isRequired,
  filter: PropTypes.shape({
    type: PropTypes.oneOf(['range']).isRequired,
    attribute: PropTypes.string.isRequired,
    values: PropTypes.shape({ exists: PropTypes.bool }),
  }).isRequired,
  //   filterIndex: PropTypes.number.isRequired,
}

export default FilterRange
