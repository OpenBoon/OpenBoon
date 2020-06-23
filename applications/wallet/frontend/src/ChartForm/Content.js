import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import { useRouter } from 'next/router'

import chartShape from '../Chart/shape'

import { colors, constants, spacing, typography } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import { capitalizeFirstLetter } from '../Metadata/helpers'
import { ACTIONS } from '../DataVisualization/reducer'

import ChartFormOptions from './Options'
import { formatFields, getCountList } from './helpers'

const ICON_SIZE = 20
const MAX_COUNT = 10

const ChartFormContent = ({ chart, chart: { type }, chartIndex, dispatch }) => {
  const {
    query: { projectId },
  } = useRouter()

  const [attribute, setAttribute] = useState('')
  const [count, setCount] = useState(5)

  const { data: fields } = useSWR(
    `/api/v1/projects/${projectId}/searches/fields/`,
  )

  const filteredFields = formatFields({ fields, type })
  const countList = getCountList({ count: MAX_COUNT })

  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        label: {
          fontWeight: typography.weight.medium,
          paddingBottom: spacing.base,
        },
        select: {
          padding: `${spacing.moderate}px ${spacing.base}px`,
          backgroundColor: colors.structure.steel,
          color: colors.structure.white,
          borderRadius: constants.borderRadius.small,
          border: 'none',
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          paddingLeft: spacing.moderate,
          MozAppearance: 'none',
          WebkitAppearance: 'none',
          backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZD0iTTE0LjI0MyA3LjU4NkwxMCAxMS44MjggNS43NTcgNy41ODYgNC4zNDMgOSAxMCAxNC42NTcgMTUuNjU3IDlsLTEuNDE0LTEuNDE0eiIgZmlsbD0iI2ZmZmZmZiIgLz4KPC9zdmc+')`,
          backgroundRepeat: `no-repeat, repeat`,
          backgroundPosition: `right ${spacing.base}px top 50%`,
          backgroundSize: ICON_SIZE,
        },
      }}
    >
      <h2
        css={{
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
        }}
      >
        {capitalizeFirstLetter({ word: type })} Visualization
      </h2>

      <div css={{ height: spacing.comfy }} />

      <label htmlFor="metadata-type-selection">Metadata Type</label>
      <select
        id="metadata-type-selection"
        defaultValue={attribute}
        onChange={({ target: { value } }) => {
          setAttribute(value)
        }}
      >
        <option value="" disabled>
          Select metadata type
        </option>
        <ChartFormOptions fields={filteredFields} type={type} />
      </select>

      <div css={{ height: spacing.spacious }} />

      <label htmlFor="visualization-count-selection">
        Select Number of Values Shown (top {MAX_COUNT} max)
      </label>
      <div css={{ display: 'flex' }}>
        <select
          id="visualization-count-selection"
          defaultValue={count}
          onChange={({ target: { value } }) => {
            setCount(value)
          }}
          css={{
            flex: 1,
          }}
        >
          {countList.map((value) => (
            <option>{value}</option>
          ))}
        </select>
        <div css={{ paddingLeft: spacing.comfy, flex: 1 }} />
      </div>

      <div css={{ height: spacing.spacious }} />

      <div css={{ display: 'flex', button: { flex: 1 } }}>
        <Button
          variant={BUTTON_VARIANTS.SECONDARY}
          onClick={() =>
            dispatch({ type: ACTIONS.DELETE, payload: { chartIndex } })
          }
        >
          Cancel
        </Button>
        <div css={{ width: spacing.base }} />
        <Button
          variant={BUTTON_VARIANTS.PRIMARY}
          isDisabled={!attribute || !count}
          onClick={() =>
            dispatch({
              type: ACTIONS.UPDATE,
              payload: {
                chartIndex,
                updatedChart: { ...chart, attribute, count },
              },
            })
          }
        >
          Save Visualization
        </Button>
      </div>
    </div>
  )
}

ChartFormContent.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
  chartIndex: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ChartFormContent
