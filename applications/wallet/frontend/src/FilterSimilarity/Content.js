import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from '../Filter/shape'

import { spacing } from '../Styles'

import { dispatch, ACTIONS } from '../Filters/helpers'
import FilterReset from '../Filter/Reset'

import { formatValue } from '../FilterRange/helpers'

import FilterSimilaritySlider from './Slider'

const THUMBNAIL_SIZE = 74

const FilterSimilarityContent = ({
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
    if (!acc || file.size < acc.size) {
      return file
    }

    return acc
  }, '')

  const largerDimension = width > height ? 'height' : 'width'
  const fileSrc = `/api/v1/projects/${projectId}/assets/${assetId}/files/category/${category}/name/${name}/`

  return (
    <div>
      <FilterReset
        projectId={projectId}
        assetId={assetId}
        filters={filters}
        filter={filter}
        filterIndex={filterIndex}
        onReset={() => {
          setValue(0.75)
        }}
      />
      <div css={{ padding: spacing.normal }}>
        <div
          css={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          <img
            css={{ [largerDimension]: THUMBNAIL_SIZE }}
            src={fileSrc}
            alt={name}
          />
        </div>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            paddingTop: spacing.normal,
            paddingBottom: spacing.normal,
          }}
        >
          <span>
            Similarity Range: {formatValue({ attribute, value }).toFixed(2)}
          </span>
        </div>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            paddingBottom: spacing.normal,
            fontFamily: 'Roboto Mono',
          }}
        >
          <span>0.01</span>
          <span>1.00</span>
        </div>
        <div css={{ padding: spacing.small }}>
          <FilterSimilaritySlider
            step={0.01}
            domain={[0.01, 1]}
            values={[value]}
            isDisabled={!!isDisabled}
            onUpdate={([newValue]) => {
              setValue(newValue)
            }}
            onChange={([newMinScore]) => {
              dispatch({
                action: ACTIONS.UPDATE_FILTER,
                payload: {
                  projectId,
                  assetId,
                  filters,
                  updatedFilter: {
                    type,
                    attribute,
                    values: { ids, minScore: newMinScore },
                    isDisabled,
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
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterSimilarityContent
