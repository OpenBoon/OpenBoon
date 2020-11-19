import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from '../Filter/shape'

import { colors, constants, spacing, typography } from '../Styles'

import Slider from '../Slider'
import { dispatch, ACTIONS, encode } from '../Filters/helpers'
import FilterReset from '../Filter/Reset'

import { formatValue, parseValue } from './helpers'

const FilterRangeContent = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    type,
    attribute,
    values: { min, max },
    isDisabled,
  },
  filterIndex,
}) => {
  const encodedFilter = encode({ filters: { type, attribute } })

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: false,
      shouldRetryOnError: false,
    },
  )

  const { results = {} } = data || {}

  const resultsMin = results?.min || 0
  const resultsMax = results?.max || 1

  const minMaxFix = resultsMin === resultsMax ? 0.001 : 0

  const domain = [resultsMin, resultsMax + minMaxFix]

  const [rangeValues, setRangeValues] = useState([
    min || resultsMin,
    max || resultsMax + minMaxFix,
  ])
  const [inputMin, setInputMin] = useState(rangeValues[0])
  const [inputMax, setInputMax] = useState(rangeValues[1])

  const saveMinValue = ({ value }) => {
    const newMin = parseValue({ value })

    if (newMin === rangeValues[0]) return

    if (newMin < domain[0] || newMin > rangeValues[1]) {
      setInputMin(rangeValues[0])
      return
    }

    setInputMin(newMin)

    setRangeValues([newMin, rangeValues[1]])

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
          values: { min: newMin, max: rangeValues[1] },
        },
        filterIndex,
      },
    })
  }

  const saveMaxValue = ({ value }) => {
    const newMax = parseValue({ value })

    if (newMax === rangeValues[1]) return

    if (newMax < rangeValues[0] || newMax > domain[1]) {
      setInputMax(rangeValues[1])
      return
    }

    setInputMax(newMax)

    setRangeValues([rangeValues[0], newMax])

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
          values: { min: rangeValues[0], max: newMax },
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
          setRangeValues(domain)
          setInputMin(domain[0])
          setInputMax(domain[1])
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
          <span>{formatValue({ attribute, value: resultsMin })}</span>
          <span>
            {formatValue({ attribute, value: resultsMax + minMaxFix })}
          </span>
        </div>
        <div css={{ padding: spacing.small }}>
          <Slider
            step={0.1}
            domain={domain}
            values={rangeValues}
            isMuted={!!isDisabled}
            isDisabled={false}
            onUpdate={(values) => {
              setRangeValues(values)
              setInputMin(parseValue({ value: values[0] }))
              setInputMax(parseValue({ value: values[1] }))
            }}
            onChange={([newMin, newMax]) => {
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
                    values: { min: newMin, max: newMax },
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
            justifyContent: 'space-around',
          }}
        >
          <label css={{ display: 'flex', alignItems: 'center' }}>
            MIN &nbsp;
            <input
              type="text"
              css={{
                textAlign: 'center',
                paddingLeft: spacing.moderate,
                paddingRight: spacing.moderate,
                paddingTop: spacing.normal,
                paddingBottom: spacing.normal,
                border: constants.borders.regular.transparent,
                borderRadius: constants.borderRadius.small,
                backgroundColor: colors.structure.lead,
                color: colors.structure.white,
                width: '60%',
                ':hover': {
                  border: constants.borders.regular.steel,
                },
                ':focus': {
                  outline: constants.borders.regular.transparent,
                  border: constants.borders.keyOneRegular,
                  color: colors.structure.coal,
                  backgroundColor: colors.structure.white,
                },
                '::placeholder': {
                  fontStyle: typography.style.italic,
                },
              }}
              value={inputMin}
              onChange={({ target: { value } }) => setInputMin(value)}
              onKeyPress={({ target: { value }, key }) => {
                if (key !== 'Enter') return
                saveMinValue({ value })
              }}
              onBlur={({ target: { value } }) => {
                saveMinValue({ value })
              }}
            />
          </label>
          <label css={{ display: 'flex', alignItems: 'center' }}>
            MAX &nbsp;
            <input
              type="text"
              css={{
                textAlign: 'center',
                paddingLeft: spacing.moderate,
                paddingRight: spacing.moderate,
                paddingTop: spacing.normal,
                paddingBottom: spacing.normal,
                border: constants.borders.regular.transparent,
                borderRadius: constants.borderRadius.small,
                backgroundColor: colors.structure.lead,
                color: colors.structure.white,
                width: '60%',
                ':hover': {
                  border: constants.borders.regular.steel,
                },
                ':focus': {
                  outline: constants.borders.regular.transparent,
                  border: constants.borders.keyOneRegular,
                  color: colors.structure.coal,
                  backgroundColor: colors.structure.white,
                },
                '::placeholder': {
                  fontStyle: typography.style.italic,
                },
              }}
              value={inputMax}
              onChange={({ target: { value } }) => setInputMax(value)}
              onKeyPress={({ target: { value }, key }) => {
                if (key !== 'Enter') return
                saveMaxValue({ value })
              }}
              onBlur={({ target: { value } }) => {
                saveMaxValue({ value })
              }}
            />
          </label>
        </div>
      </div>
    </div>
  )
}

FilterRangeContent.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterRangeContent
