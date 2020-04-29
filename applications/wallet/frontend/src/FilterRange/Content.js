import PropTypes from 'prop-types'
import useSWR from 'swr'

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

  return (
    <ul css={{ margin: 0 }}>
      {Object.entries(results).map(([key, value]) => (
        <li key={key}>
          {key}: {value}
        </li>
      ))}
    </ul>
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
