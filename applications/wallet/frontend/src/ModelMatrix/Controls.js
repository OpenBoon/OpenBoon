import { useState } from 'react'
import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

import { parseValue } from '../FilterRange/helpers'

import RadioGroup from '../Radio/Group'

import InputRange, { VARIANTS } from '../Input/Range'

import { DEFAULT_MIN, DEFAULT_MAX } from './reducer'

const ModelMatrixControls = ({ isNormalized, dispatch }) => {
  const domain = [DEFAULT_MIN, DEFAULT_MAX]

  const [rangeValues, setRangeValues] = useState([DEFAULT_MIN, DEFAULT_MAX])
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

    dispatch({ minScore: newMin })
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

    dispatch({ maxScore: newMax })
  }

  return (
    <form
      method="post"
      onSubmit={(event) => event.preventDefault()}
      css={{
        display: 'flex',
        paddingLeft: spacing.spacious,
      }}
    >
      <RadioGroup
        legend="View"
        options={[
          {
            value: 'normalized',
            label: 'Normalized',
            legend: '',
            initialValue: isNormalized,
          },
          {
            value: 'absolute',
            label: 'Absolute',
            legend: '',
            initialValue: !isNormalized,
          },
        ]}
        onClick={({ value }) =>
          dispatch({ isNormalized: value === 'normalized' })
        }
      />

      <div
        css={{
          color: colors.structure.zinc,
          display: 'flex',
          alignItems: 'center',
          paddingLeft: spacing.spacious,
        }}
      >
        <span
          css={{
            color: colors.structure.white,
            fontWeight: typography.weight.bold,
            paddingRight: spacing.normal,
          }}
        >
          Confidence:
        </span>
        <div css={{ paddingRight: spacing.comfy }}>
          <InputRange
            label="MIN"
            value={inputMin}
            onChange={({ target: { value } }) => setInputMin(value)}
            onKeyPress={({ target: { value }, key }) => {
              if (key !== 'Enter') return
              saveMinValue({ value })
            }}
            onBlur={({ target: { value } }) => {
              saveMinValue({ value })
            }}
            variant={VARIANTS.SECONDARY}
            style={{ label: { paddingRight: spacing.comfy } }}
          />
        </div>
        <InputRange
          label="MAX"
          value={inputMax}
          onChange={({ target: { value } }) => setInputMax(value)}
          onKeyPress={({ target: { value }, key }) => {
            if (key !== 'Enter') return
            saveMaxValue({ value })
          }}
          onBlur={({ target: { value } }) => {
            saveMaxValue({ value })
          }}
          variant={VARIANTS.SECONDARY}
        />
      </div>
    </form>
  )
}

ModelMatrixControls.propTypes = {
  isNormalized: PropTypes.bool.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelMatrixControls
