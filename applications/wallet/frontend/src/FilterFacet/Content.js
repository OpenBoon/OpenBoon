import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from '../Filter/shape'

import { spacing } from '../Styles'

import FilterReset from '../Filter/Reset'
import FilterSearch from '../Filter/Search'
import FilterBuckets from '../FilterBuckets'

import { encode } from '../Filters/helpers'

export const noop = () => {}

const FilterFacet = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filter: { type, attribute },
  filterIndex,
}) => {
  const [searchString, setSearchString] = useState('')

  const encodedFilter = encode({ filters: { type, attribute } })

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )
  const { results } = data || {}

  const { buckets = [] } = results || {}

  return (
    <>
      <FilterReset
        pathname={pathname}
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        onReset={noop}
      />

      <div css={{ height: spacing.moderate }} />

      <FilterSearch
        placeholder="Filter facets"
        searchString={searchString}
        onChange={({ value }) => {
          setSearchString(value)
        }}
      />

      <FilterBuckets
        pathname={pathname}
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        buckets={buckets}
        searchString={searchString}
      />
    </>
  )
}

FilterFacet.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterFacet
