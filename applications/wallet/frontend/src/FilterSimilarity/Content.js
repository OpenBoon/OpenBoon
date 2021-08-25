import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from '../Filter/shape'

import { spacing, typography } from '../Styles'

import { dispatch, ACTIONS } from '../Filters/helpers'
import FilterReset from '../Filter/Reset'

import { formatValue } from '../FilterRange/helpers'

import Slider from '../Slider'

const THUMBNAIL_SIZE = 74

const FilterSimilarityContent = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    type,
    attribute,
    values: { ids, minScore },
    isDisabled,
  },
  filterIndex,
}) => {
  const [value, setValue] = useState(minScore || 0.75)

  const {
    data: {
      metadata: { files },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${ids[0]}`, {
    revalidateOnFocus: false,
    revalidateOnReconnect: false,
    shouldRetryOnError: false,
  })

  const {
    name,
    category,
    attrs: { width, height },
  } = files.reduce((acc, file) => {
    if (!file.mimetype.includes('image')) return acc

    if (acc && acc.size < file.size) return acc

    return file
  }, undefined)

  const largerDimension = width > height ? 'height' : 'width'
  const fileSrc = `/api/v1/projects/${projectId}/assets/${ids[0]}/files/category/${category}/name/${name}/`

  return (
    <div>
      <FilterReset
        pathname={pathname}
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        onReset={() => {
          setValue(0.75)
        }}
      />

      <div
        css={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: spacing.normal,
        }}
      >
        <img
          css={{ [largerDimension]: THUMBNAIL_SIZE }}
          src={fileSrc}
          alt={name}
        />
      </div>

      <div css={{ fontStyle: typography.style.italic }}>
        Similarity Range: {formatValue({ attribute, value }).toFixed(2)}
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
          <span>0.01</span>
          <span>1.00</span>
        </div>

        <div css={{ padding: spacing.small }}>
          <Slider
            mode="max"
            step={0.01}
            domain={[0.01, 1]}
            values={[value]}
            isMuted={!!isDisabled}
            isDisabled={false}
            onUpdate={([newValue]) => {
              setValue(newValue)
            }}
            onChange={([newMinScore]) => {
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
                    values: { ids, minScore: newMinScore },
                  },
                  filterIndex,
                },
              })
            }}
          />
        </div>
      </div>
    </div>
  )
}

FilterSimilarityContent.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterSimilarityContent
