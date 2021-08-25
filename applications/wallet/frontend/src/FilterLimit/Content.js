import { useState } from 'react'
import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { colors, spacing, typography } from '../Styles'

import Slider from '../Slider'
import InputRange, { VARIANTS } from '../Input/Range'
import { dispatch, ACTIONS } from '../Filters/helpers'
import FilterReset from '../Filter/Reset'

const FilterLimitContent = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    type,
    attribute,
    values: { maxAssets },
    isDisabled,
  },
  filterIndex,
}) => {
  const min = 0
  const max = 10_000

  const domain = [min, max]

  const [input, setInput] = useState(maxAssets)

  const saveValue = ({ value }) => {
    const newLimit = parseFloat(value)

    if (newLimit === maxAssets) return

    if (newLimit < min || newLimit > max) {
      setInput(maxAssets)
      return
    }

    setInput(newLimit)

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
          values: { maxAssets: newLimit },
        },
        filterIndex,
      },
    })
  }

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
          setInput(max)
        }}
      />
      <div css={{ padding: spacing.normal }}>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            paddingBottom: spacing.normal,
            fontFamily: typography.family.mono,
          }}
        >
          <span>0</span>
          <span>10,000</span>
        </div>
        <div css={{ padding: spacing.small }}>
          <Slider
            mode="min"
            step={1}
            domain={domain}
            values={[maxAssets]}
            isMuted={!!isDisabled}
            isDisabled={false}
            onUpdate={(values) => {
              setInput(values[0])
            }}
            onChange={(values) => {
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
                    values: { maxAssets: values[0] },
                  },
                  filterIndex,
                },
              })
            }}
          />
        </div>
        <div
          css={{
            paddingTop: spacing.comfy,
            color: colors.structure.zinc,
            display: 'flex',
            justifyContent: 'center',
            label: {
              flex: 1,
              display: 'flex',
              justifyContent: 'center',
              input: {
                width: '40%',
              },
            },
          }}
        >
          <InputRange
            label="MAX NUMBER OF ASSETS"
            value={input}
            onChange={({ target: { value } }) => setInput(value)}
            onKeyPress={({ target: { value }, key }) => {
              if (key !== 'Enter') return
              saveValue({ value })
            }}
            onBlur={({ target: { value } }) => {
              saveValue({ value })
            }}
            variant={VARIANTS.PRIMARY}
          />
        </div>
      </div>
    </div>
  )
}

FilterLimitContent.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterLimitContent
