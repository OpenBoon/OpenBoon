import PropTypes from 'prop-types'
import useSWR from 'swr'

const FilterFacet = ({
  projectId,
  //   assetId,
  //   filters,
  //   filter,
  filter: { type, attribute },
  //   filterIndex,
}) => {
  const encodedFilter = btoa(JSON.stringify({ type, attribute }))

  const {
    data: {
      results: { buckets },
    },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
  )

  return (
    <ul css={{ margin: 0 }}>
      {buckets.map(({ key, docCount }) => (
        <li key={key}>
          {key}: {docCount}
        </li>
      ))}
    </ul>
  )
}

FilterFacet.propTypes = {
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
    type: PropTypes.oneOf(['facet']).isRequired,
    attribute: PropTypes.string.isRequired,
    values: PropTypes.shape({ exists: PropTypes.bool }),
  }).isRequired,
  //   filterIndex: PropTypes.number.isRequired,
}

export default FilterFacet
